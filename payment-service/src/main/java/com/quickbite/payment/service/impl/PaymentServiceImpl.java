package com.quickbite.payment.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.payment.client.OrderServiceClient;
import com.quickbite.payment.dto.CodPaymentRequest;
import com.quickbite.payment.dto.CreatePaymentOrderRequest;
import com.quickbite.payment.dto.CreatePaymentOrderResponse;
import com.quickbite.payment.dto.OrderPaymentStatusRequest;
import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.RefundRequest;
import com.quickbite.payment.dto.VerifyPaymentRequest;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.enums.PaymentMode;
import com.quickbite.payment.enums.PaymentStatus;
import com.quickbite.payment.exception.PaymentException;
import com.quickbite.payment.exception.ResourceNotFoundException;
import com.quickbite.payment.gateway.RazorpayGateway;
import com.quickbite.payment.repository.PaymentRepository;
import com.quickbite.payment.service.PaymentService;
import com.quickbite.payment.service.WalletService;
import com.razorpay.Order;
import com.razorpay.Refund;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayGateway razorpayGateway;
    private final OrderServiceClient orderServiceClient;
    private final WalletService walletService;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              RazorpayGateway razorpayGateway,
                              OrderServiceClient orderServiceClient,
                              WalletService walletService,
                              ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.razorpayGateway = razorpayGateway;
        this.orderServiceClient = orderServiceClient;
        this.walletService = walletService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CreatePaymentOrderResponse createRazorpayOrder(CreatePaymentOrderRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.orderId())
            .map(existingPayment -> preparePendingRazorpayPayment(existingPayment, request))
            .orElseGet(() -> buildPendingRazorpayPayment(request));

        Order razorpayOrder = razorpayGateway.createOrder(request.amount(), request.currency(), "QB-" + request.orderId());
        payment.setRazorpayOrderId(razorpayOrder.get("id"));

        Payment savedPayment = paymentRepository.save(payment);
        long amountInPaise = request.amount().movePointRight(2).longValueExact();

        return new CreatePaymentOrderResponse(
            savedPayment.getPaymentId(),
            savedPayment.getOrderId(),
            savedPayment.getRazorpayOrderId(),
            amountInPaise,
            savedPayment.getCurrency(),
            razorpayGateway.getKeyId()
        );
    }

    @Override
    @Transactional
    public PaymentResponse verifyRazorpayPayment(VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.orderId())
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order ID: " + request.orderId()));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return mapToPaymentResponse(payment);
        }
        if (!request.razorpayOrderId().equals(payment.getRazorpayOrderId())) {
            throw new PaymentException("Razorpay order ID does not match the local payment record");
        }

        boolean validSignature = razorpayGateway.verifyPaymentSignature(
            request.razorpayOrderId(),
            request.razorpayPaymentId(),
            request.razorpaySignature()
        );

        if (!validSignature) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentException("Invalid Razorpay payment signature");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setMode(resolveOnlinePaymentMode(payment));
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setRazorpaySignature(request.razorpaySignature());
        payment.setTransactionId(request.razorpayPaymentId());
        payment.setPaidAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);
        orderServiceClient.updateOrderPaymentStatus(savedPayment.getOrderId(), new OrderPaymentStatusRequest(savedPayment.getStatus().name()));
        return mapToPaymentResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse createCodPayment(CodPaymentRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.orderId())
            .map(existingPayment -> prepareCodPayment(existingPayment, request))
            .orElseGet(() -> buildCodPayment(request));

        Payment savedPayment = paymentRepository.save(payment);
        return mapToPaymentResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(RefundRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.orderId())
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order ID: " + request.orderId()));

        if (payment.getMode() == PaymentMode.COD) {
            return mapToPaymentResponse(payment);
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return mapToPaymentResponse(payment);
        }
        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw new PaymentException("Only paid payments can be refunded");
        }

        if (payment.getMode() == PaymentMode.WALLET && payment.getRazorpayPaymentId() == null) {
            walletService.creditRefund(
                payment.getCustomerId(),
                payment.getAmount(),
                "Refund for order " + payment.getOrderId() + ": " + request.reason(),
                "REFUND-" + payment.getOrderId() + "-" + UUID.randomUUID()
            );
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
        } else {
            if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank()) {
                throw new PaymentException("Razorpay payment ID is missing for this refund");
            }
            Refund refund = razorpayGateway.refundPayment(payment.getRazorpayPaymentId(), payment.getAmount());
            String refundStatus = refund.has("status") ? refund.get("status").toString() : "processed";
            if ("processed".equalsIgnoreCase(refundStatus)) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAt(LocalDateTime.now());
            } else {
                payment.setStatus(PaymentStatus.REFUND_PENDING);
            }
        }

        Payment savedPayment = paymentRepository.save(payment);
        orderServiceClient.updateOrderPaymentStatus(savedPayment.getOrderId(), new OrderPaymentStatusRequest(savedPayment.getStatus().name()));
        return mapToPaymentResponse(savedPayment);
    }

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order ID: " + orderId));
        return mapToPaymentResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPaymentsByCustomerId(Long customerId) {
        return paymentRepository.findByCustomerId(customerId).stream()
            .map(this::mapToPaymentResponse)
            .toList();
    }

    @Override
    public List<PaymentResponse> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status).stream()
            .map(this::mapToPaymentResponse)
            .toList();
    }

    @Override
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
            .sorted((left, right) -> Long.compare(right.getPaymentId(), left.getPaymentId()))
            .map(this::mapToPaymentResponse)
            .toList();
    }

    @Override
    @Transactional
    public void handleWebhookEvent(String payload, String signature) {
        boolean validSignature = razorpayGateway.verifyWebhookSignature(payload, signature);
        if (!validSignature) {
            throw new PaymentException("Invalid webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText("");

            switch (event) {
                case "payment.captured" -> handlePaymentCaptured(root);
                case "payment.failed" -> handlePaymentFailed(root);
                case "refund.processed" -> handleRefundProcessed(root);
                default -> {
                    // Unknown events are intentionally ignored.
                }
            }
        } catch (Exception exception) {
            throw new PaymentException("Unable to process Razorpay webhook payload");
        }
    }

    private void handlePaymentCaptured(JsonNode root) {
        JsonNode paymentNode = root.path("payload").path("payment").path("entity");
        String razorpayPaymentId = paymentNode.path("id").asText(null);
        String razorpayOrderId = paymentNode.path("order_id").asText(null);

        if (razorpayPaymentId == null && razorpayOrderId == null) {
            return;
        }

        Payment payment = findByWebhookIds(razorpayPaymentId, razorpayOrderId);
        if (payment == null) {
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setMode(resolveOnlinePaymentMode(payment));
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setTransactionId(razorpayPaymentId);
        if (payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);
        orderServiceClient.updateOrderPaymentStatus(payment.getOrderId(), new OrderPaymentStatusRequest(payment.getStatus().name()));
    }

    private void handlePaymentFailed(JsonNode root) {
        JsonNode paymentNode = root.path("payload").path("payment").path("entity");
        String razorpayPaymentId = paymentNode.path("id").asText(null);
        String razorpayOrderId = paymentNode.path("order_id").asText(null);

        Payment payment = findByWebhookIds(razorpayPaymentId, razorpayOrderId);
        if (payment == null) {
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
    }

    private void handleRefundProcessed(JsonNode root) {
        JsonNode refundNode = root.path("payload").path("refund").path("entity");
        String razorpayPaymentId = refundNode.path("payment_id").asText(null);
        if (razorpayPaymentId == null) {
            return;
        }

        paymentRepository.findByRazorpayPaymentId(razorpayPaymentId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            orderServiceClient.updateOrderPaymentStatus(payment.getOrderId(), new OrderPaymentStatusRequest(payment.getStatus().name()));
        });
    }

    private Payment findByWebhookIds(String razorpayPaymentId, String razorpayOrderId) {
        if (razorpayPaymentId != null) {
            return paymentRepository.findByRazorpayPaymentId(razorpayPaymentId).orElse(null);
        }
        if (razorpayOrderId != null) {
            return paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        }
        return null;
    }

    private Payment buildPendingRazorpayPayment(CreatePaymentOrderRequest request) {
        return Payment.builder()
            .orderId(request.orderId())
            .customerId(request.customerId())
            .amount(request.amount())
            .status(PaymentStatus.PENDING)
            .mode(PaymentMode.CARD)
            .currency(request.currency())
            .build();
    }

    private Payment preparePendingRazorpayPayment(Payment payment, CreatePaymentOrderRequest request) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new PaymentException("Payment is already completed for this order");
        }
        payment.setCustomerId(request.customerId());
        payment.setAmount(request.amount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMode(PaymentMode.CARD);
        payment.setCurrency(request.currency());
        payment.setRazorpayPaymentId(null);
        payment.setRazorpaySignature(null);
        payment.setTransactionId(null);
        payment.setPaidAt(null);
        payment.setRefundedAt(null);
        return payment;
    }

    private Payment buildCodPayment(CodPaymentRequest request) {
        return Payment.builder()
            .orderId(request.orderId())
            .customerId(request.customerId())
            .amount(request.amount())
            .status(PaymentStatus.PENDING)
            .mode(PaymentMode.COD)
            .currency("INR")
            .build();
    }

    private Payment prepareCodPayment(Payment payment, CodPaymentRequest request) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new PaymentException("Payment is already completed for this order");
        }
        payment.setCustomerId(request.customerId());
        payment.setAmount(request.amount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMode(PaymentMode.COD);
        payment.setCurrency("INR");
        payment.setRazorpayOrderId(null);
        payment.setRazorpayPaymentId(null);
        payment.setRazorpaySignature(null);
        payment.setTransactionId(null);
        payment.setPaidAt(null);
        payment.setRefundedAt(null);
        return payment;
    }

    private PaymentMode resolveOnlinePaymentMode(Payment payment) {
        return payment.getMode() == PaymentMode.UPI ? PaymentMode.UPI : PaymentMode.CARD;
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return new PaymentResponse(
            payment.getPaymentId(),
            payment.getOrderId(),
            payment.getCustomerId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getMode(),
            payment.getTransactionId(),
            payment.getRazorpayOrderId(),
            payment.getRazorpayPaymentId(),
            payment.getCurrency(),
            payment.getPaidAt(),
            payment.getRefundedAt(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}
