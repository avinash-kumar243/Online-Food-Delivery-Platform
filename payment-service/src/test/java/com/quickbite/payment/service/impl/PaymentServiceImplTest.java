package com.quickbite.payment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.payment.client.OrderServiceClient;
import com.quickbite.payment.dto.CodPaymentRequest;
import com.quickbite.payment.dto.CreatePaymentOrderRequest;
import com.quickbite.payment.dto.CreatePaymentOrderResponse;
import com.quickbite.payment.dto.OrderSnapshotDto;
import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.RefundRequest;
import com.quickbite.payment.dto.VerifyPaymentRequest;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.enums.PaymentMode;
import com.quickbite.payment.enums.PaymentStatus;
import com.quickbite.payment.exception.PaymentException;
import com.quickbite.payment.exception.ResourceNotFoundException;
import com.quickbite.payment.gateway.RazorpayGateway;
import com.quickbite.payment.messaging.GenericEventPublisher;
import com.quickbite.payment.messaging.dto.PaymentEventDTO;
import com.quickbite.payment.repository.PaymentRepository;
import com.quickbite.payment.service.WalletService;
import com.razorpay.Order;
import com.razorpay.Refund;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RazorpayGateway razorpayGateway;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private WalletService walletService;

    @Mock
    private GenericEventPublisher eventPublisher;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(paymentService, "entityManager", entityManager);
    }

    @Test
    void createRazorpayOrder_CreatesPendingPaymentAndReturnsCheckoutPayload() {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(1L, 11L, new BigDecimal("100.00"), "card", "INR");
        when(orderServiceClient.getOrderById(1L)).thenReturn(orderSnapshot(1L, 11L, new BigDecimal("499.00")));
        when(paymentRepository.findByOrderIdForUpdate(1L)).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(10L);
            return payment;
        });

        Order razorpayOrder = org.mockito.Mockito.mock(Order.class);
        when(razorpayOrder.get("id")).thenReturn("rzp_order_1");
        when(razorpayGateway.createOrder(new BigDecimal("499.00"), "INR", "QB-1")).thenReturn(razorpayOrder);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(razorpayGateway.getKeyId()).thenReturn("rzp_test_key");

        CreatePaymentOrderResponse response = paymentService.createRazorpayOrder(request);

        assertEquals(10L, response.paymentId());
        assertEquals(1L, response.orderId());
        assertEquals("rzp_order_1", response.razorpayOrderId());
        assertEquals(49900L, response.amount());
        assertEquals("INR", response.currency());
        assertEquals("rzp_test_key", response.keyId());
    }

    @Test
    void createRazorpayOrder_RejectsAlreadyPaidPayment() {
        CreatePaymentOrderRequest request = new CreatePaymentOrderRequest(1L, 11L, new BigDecimal("100.00"), "card", "INR");
        Payment existing = payment(7L, 1L, 11L, new BigDecimal("100.00"), PaymentStatus.PAID, PaymentMode.CARD);

        when(orderServiceClient.getOrderById(1L)).thenReturn(orderSnapshot(1L, 11L, new BigDecimal("100.00")));
        when(paymentRepository.findByOrderIdForUpdate(1L)).thenReturn(Optional.of(existing));

        PaymentException exception = assertThrows(PaymentException.class, () -> paymentService.createRazorpayOrder(request));

        assertEquals("Payment is already completed for this order", exception.getMessage());
    }

    @Test
    void verifyRazorpayPayment_MarksPaymentFailedWhenSignatureIsInvalid() {
        Payment payment = payment(5L, 1L, 11L, new BigDecimal("250.00"), PaymentStatus.PENDING, PaymentMode.CARD);
        payment.setRazorpayOrderId("rzp_order_1");
        VerifyPaymentRequest request = new VerifyPaymentRequest(1L, "rzp_order_1", "rzp_payment_1", "bad-signature");

        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));
        when(razorpayGateway.verifyPaymentSignature("rzp_order_1", "rzp_payment_1", "bad-signature")).thenReturn(false);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(orderServiceClient.getOrderById(1L)).thenReturn(orderSnapshot(1L, 11L, new BigDecimal("250.00")));

        PaymentException exception = assertThrows(PaymentException.class, () -> paymentService.verifyRazorpayPayment(request));

        assertEquals("Invalid Razorpay payment signature", exception.getMessage());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(eventPublisher).send(eq("payment.failed"), any(PaymentEventDTO.class));
    }

    @Test
    void verifyRazorpayPayment_MarksPaymentPaidAndPublishesSuccessEvent() {
        Payment payment = payment(5L, 1L, 11L, new BigDecimal("250.00"), PaymentStatus.PENDING, PaymentMode.UPI);
        payment.setRazorpayOrderId("rzp_order_1");
        VerifyPaymentRequest request = new VerifyPaymentRequest(1L, "rzp_order_1", "rzp_payment_1", "good-signature");

        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));
        when(razorpayGateway.verifyPaymentSignature("rzp_order_1", "rzp_payment_1", "good-signature")).thenReturn(true);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(orderServiceClient.getOrderById(1L)).thenReturn(orderSnapshot(1L, 11L, new BigDecimal("250.00")));

        PaymentResponse response = paymentService.verifyRazorpayPayment(request);

        assertEquals(PaymentStatus.PAID, response.status());
        assertEquals(PaymentMode.UPI, response.mode());
        assertEquals("rzp_payment_1", response.transactionId());
        assertEquals("rzp_payment_1", response.razorpayPaymentId());
        assertNotNull(response.paidAt());
        verify(eventPublisher).send(eq("payment.success"), any(PaymentEventDTO.class));
    }

    @Test
    void verifyRazorpayPayment_RejectsOrderMismatch() {
        Payment payment = payment(5L, 1L, 11L, new BigDecimal("250.00"), PaymentStatus.PENDING, PaymentMode.CARD);
        payment.setRazorpayOrderId("local-order");

        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        PaymentException exception = assertThrows(PaymentException.class,
            () -> paymentService.verifyRazorpayPayment(new VerifyPaymentRequest(1L, "remote-order", "payment", "signature")));

        assertEquals("Razorpay order ID does not match the local payment record", exception.getMessage());
    }

    @Test
    void verifyRazorpayPayment_ReturnsExistingResponseWhenAlreadyPaid() {
        Payment payment = payment(5L, 1L, 11L, new BigDecimal("250.00"), PaymentStatus.PAID, PaymentMode.CARD);
        payment.setRazorpayOrderId("rzp_order_1");

        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.verifyRazorpayPayment(
            new VerifyPaymentRequest(1L, "rzp_order_1", "rzp_payment_1", "signature")
        );

        assertEquals(PaymentStatus.PAID, response.status());
        verify(razorpayGateway, never()).verifyPaymentSignature(any(), any(), any());
    }

    @Test
    void createCodPayment_ReusesPendingPayment() {
        CodPaymentRequest request = new CodPaymentRequest(9L, 22L, new BigDecimal("300.00"));
        Payment existing = payment(12L, 9L, 22L, new BigDecimal("100.00"), PaymentStatus.PENDING, PaymentMode.CARD);
        existing.setRazorpayOrderId("old-order");
        existing.setRazorpayPaymentId("old-payment");
        existing.setRazorpaySignature("old-signature");
        existing.setTransactionId("old-transaction");
        existing.setPaidAt(LocalDateTime.now());
        existing.setRefundedAt(LocalDateTime.now());

        when(orderServiceClient.getOrderById(9L)).thenReturn(orderSnapshot(9L, 22L, new BigDecimal("300.00")));
        when(paymentRepository.findByOrderIdForUpdate(9L)).thenReturn(Optional.of(existing));
        when(paymentRepository.save(existing)).thenReturn(existing);

        PaymentResponse response = paymentService.createCodPayment(request);

        assertEquals(PaymentMode.COD, response.mode());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("INR", response.currency());
        assertNull(response.transactionId());
        assertNull(response.razorpayOrderId());
    }

    @Test
    void refundPayment_CreditsWalletRefundsImmediately() {
        Payment payment = payment(4L, 2L, 21L, new BigDecimal("175.00"), PaymentStatus.PAID, PaymentMode.WALLET);

        when(paymentRepository.findByOrderId(2L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentResponse response = paymentService.refundPayment(new RefundRequest(2L, "Customer cancelled"));

        assertEquals(PaymentStatus.REFUNDED, response.status());
        assertNotNull(response.refundedAt());
        verify(walletService).creditRefund(eq(21L), eq(new BigDecimal("175.00")), eq("Refund for order 2: Customer cancelled"), any(String.class));
    }

    @Test
    void refundPayment_ReturnsCodPaymentWithoutChanges() {
        Payment payment = payment(4L, 2L, 21L, new BigDecimal("175.00"), PaymentStatus.PENDING, PaymentMode.COD);

        when(paymentRepository.findByOrderId(2L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.refundPayment(new RefundRequest(2L, "Not needed"));

        assertEquals(PaymentMode.COD, response.mode());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refundPayment_ThrowsWhenOnlineRefundHasNoRazorpayPaymentId() {
        Payment payment = payment(4L, 2L, 21L, new BigDecimal("175.00"), PaymentStatus.PAID, PaymentMode.CARD);

        when(paymentRepository.findByOrderId(2L)).thenReturn(Optional.of(payment));

        PaymentException exception = assertThrows(PaymentException.class,
            () -> paymentService.refundPayment(new RefundRequest(2L, "Customer cancelled")));

        assertEquals("Razorpay payment ID is missing for this refund", exception.getMessage());
    }

    @Test
    void refundPayment_ReturnsExistingRefundedPayment() {
        Payment payment = payment(4L, 2L, 21L, new BigDecimal("175.00"), PaymentStatus.REFUNDED, PaymentMode.CARD);

        when(paymentRepository.findByOrderId(2L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.refundPayment(new RefundRequest(2L, "Already refunded"));

        assertEquals(PaymentStatus.REFUNDED, response.status());
        verify(razorpayGateway, never()).refundPayment(any(), any());
    }

    @Test
    void refundPayment_ProcessesRazorpayRefund() {
        Payment payment = payment(4L, 2L, 21L, new BigDecimal("175.00"), PaymentStatus.PAID, PaymentMode.CARD);
        payment.setRazorpayPaymentId("rzp_payment_9");

        Refund refund = org.mockito.Mockito.mock(Refund.class);
        when(refund.has("status")).thenReturn(true);
        when(refund.get("status")).thenReturn("processed");
        when(paymentRepository.findByOrderId(2L)).thenReturn(Optional.of(payment));
        when(razorpayGateway.refundPayment("rzp_payment_9", new BigDecimal("175.00"))).thenReturn(refund);
        when(paymentRepository.save(payment)).thenReturn(payment);

        PaymentResponse response = paymentService.refundPayment(new RefundRequest(2L, "Restaurant issue"));

        assertEquals(PaymentStatus.REFUNDED, response.status());
        assertNotNull(response.refundedAt());
    }

    @Test
    void getAllPayments_ReturnsDescendingByPaymentId() {
        Payment first = payment(1L, 100L, 1L, new BigDecimal("100.00"), PaymentStatus.PAID, PaymentMode.CARD);
        Payment second = payment(9L, 101L, 1L, new BigDecimal("90.00"), PaymentStatus.PENDING, PaymentMode.UPI);

        when(paymentRepository.findAll()).thenReturn(List.of(first, second));

        List<PaymentResponse> responses = paymentService.getAllPayments();

        assertEquals(List.of(9L, 1L), responses.stream().map(PaymentResponse::paymentId).toList());
    }

    @Test
    void getPaymentByOrderId_ThrowsWhenPaymentIsMissing() {
        when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> paymentService.getPaymentByOrderId(999L));

        assertEquals("Payment not found for order ID: 999", exception.getMessage());
    }

    @Test
    void handleWebhookEvent_RejectsInvalidSignature() {
        when(razorpayGateway.verifyWebhookSignature("{}", "bad")).thenReturn(false);

        PaymentException exception = assertThrows(PaymentException.class, () -> paymentService.handleWebhookEvent("{}", "bad"));

        assertEquals("Invalid webhook signature", exception.getMessage());
    }

    @Test
    void handleWebhookEvent_CapturesPaymentByRazorpayOrderId() {
        Payment payment = payment(4L, 2L, 21L, new BigDecimal("175.00"), PaymentStatus.PENDING, PaymentMode.CARD);
        payment.setRazorpayOrderId("rzp_order_44");

        when(razorpayGateway.verifyWebhookSignature(any(String.class), eq("good"))).thenReturn(true);
        when(paymentRepository.findByRazorpayOrderId("rzp_order_44")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(orderServiceClient.getOrderById(2L)).thenReturn(orderSnapshot(2L, 21L, new BigDecimal("175.00")));

        paymentService.handleWebhookEvent("""
            {"event":"payment.captured","payload":{"payment":{"entity":{"id":"rzp_payment_44","order_id":"rzp_order_44"}}}}
            """, "good");

        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals("rzp_payment_44", payment.getTransactionId());
        verify(eventPublisher).send(eq("payment.success"), any(PaymentEventDTO.class));
    }

    @Test
    void handleWebhookEvent_ProcessesRefundNotification() {
        Payment payment = payment(4L, 2L, 21L, new BigDecimal("175.00"), PaymentStatus.PAID, PaymentMode.CARD);
        payment.setRazorpayPaymentId("rzp_payment_44");

        when(razorpayGateway.verifyWebhookSignature(any(String.class), eq("good"))).thenReturn(true);
        when(paymentRepository.findByRazorpayPaymentId("rzp_payment_44")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        paymentService.handleWebhookEvent("""
            {"event":"refund.processed","payload":{"refund":{"entity":{"payment_id":"rzp_payment_44"}}}}
            """, "good");

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertNotNull(payment.getRefundedAt());
    }

    @Test
    void handleWebhookEvent_ProcessesPaymentFailedNotification() {
        Payment payment = payment(4L, 2L, 21L, new BigDecimal("175.00"), PaymentStatus.PENDING, PaymentMode.CARD);
        payment.setRazorpayOrderId("rzp_order_failed");

        when(razorpayGateway.verifyWebhookSignature(any(String.class), eq("good"))).thenReturn(true);
        when(paymentRepository.findByRazorpayOrderId("rzp_order_failed")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(orderServiceClient.getOrderById(2L)).thenReturn(orderSnapshot(2L, 21L, new BigDecimal("175.00")));

        paymentService.handleWebhookEvent("""
            {"event":"payment.failed","payload":{"payment":{"entity":{"id":"rzp_payment_failed","order_id":"rzp_order_failed"}}}}
            """, "good");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(eventPublisher).send(eq("payment.failed"), any(PaymentEventDTO.class));
    }

    @Test
    void handleWebhookEvent_IgnoresUnknownEvents() {
        when(razorpayGateway.verifyWebhookSignature(any(String.class), eq("good"))).thenReturn(true);

        paymentService.handleWebhookEvent("""
            {"event":"subscription.unknown","payload":{}}
            """, "good");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void handleWebhookEvent_ThrowsFriendlyErrorForMalformedPayload() {
        when(razorpayGateway.verifyWebhookSignature(any(String.class), eq("good"))).thenReturn(true);

        PaymentException exception = assertThrows(PaymentException.class,
            () -> paymentService.handleWebhookEvent("not-json", "good"));

        assertEquals("Unable to process Razorpay webhook payload", exception.getMessage());
    }

    private OrderSnapshotDto orderSnapshot(Long orderId, Long customerId, BigDecimal amount) {
        return new OrderSnapshotDto(orderId, customerId, 301L, 401L, amount);
    }

    private Payment payment(Long paymentId, Long orderId, Long customerId, BigDecimal amount,
                            PaymentStatus status, PaymentMode mode) {
        Payment payment = Payment.builder()
            .paymentId(paymentId)
            .orderId(orderId)
            .customerId(customerId)
            .amount(amount)
            .status(status)
            .mode(mode)
            .currency("INR")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        payment.setTransactionId("tx-" + paymentId);
        return payment;
    }
}
