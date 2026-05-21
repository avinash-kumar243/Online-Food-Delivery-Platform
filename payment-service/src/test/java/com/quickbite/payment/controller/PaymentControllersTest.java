package com.quickbite.payment.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.payment.dto.CreatePaymentOrderResponse;
import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.WalletBalanceResponse;
import com.quickbite.payment.dto.WalletStatementResponse;
import com.quickbite.payment.enums.PaymentMode;
import com.quickbite.payment.enums.PaymentStatus;
import com.quickbite.payment.enums.WalletTransactionType;
import com.quickbite.payment.exception.GlobalExceptionHandler;
import com.quickbite.payment.exception.PaymentException;
import com.quickbite.payment.exception.ResourceNotFoundException;
import com.quickbite.payment.service.PaymentService;
import com.quickbite.payment.service.WalletService;

@ExtendWith(MockitoExtension.class)
class PaymentControllersTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private WalletService walletService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PaymentController(paymentService),
                new AdminPaymentController(paymentService),
                new WalletController(walletService),
                new RazorpayWebhookController(paymentService)
            )
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void createRazorpayOrder_ReturnsCreatedResponse() throws Exception {
        when(paymentService.createRazorpayOrder(any())).thenReturn(
            new CreatePaymentOrderResponse(10L, 1L, "rzp_order_1", 49900L, "INR", "rzp_key")
        );

        mockMvc.perform(post("/api/v1/payments/razorpay/create-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"orderId":1,"customerId":11,"amount":499.00,"paymentMode":"CARD","currency":"INR"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.razorpayOrderId", is("rzp_order_1")));
    }

    @Test
    void createRazorpayOrder_ReturnsValidationErrorForMissingOrderId() throws Exception {
        mockMvc.perform(post("/api/v1/payments/razorpay/create-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerId":11,"amount":499.00,"paymentMode":"CARD","currency":"INR"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("Order ID is required")));
    }

    @Test
    void refundPayment_UsesGlobalExceptionHandler() throws Exception {
        when(paymentService.refundPayment(any())).thenThrow(new PaymentException("Refund failed"));

        mockMvc.perform(post("/api/v1/payments/refund")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"orderId":1,"reason":"Customer cancelled"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("Refund failed")));
    }

    @Test
    void getPaymentByOrder_ReturnsNotFoundResponse() throws Exception {
        when(paymentService.getPaymentByOrderId(99L)).thenThrow(new ResourceNotFoundException("Payment not found"));

        mockMvc.perform(get("/api/v1/payments/order/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message", is("Payment not found")));
    }

    @Test
    void getAllPayments_ReturnsAdminList() throws Exception {
        when(paymentService.getAllPayments()).thenReturn(List.of(paymentResponse()));

        mockMvc.perform(get("/api/v1/admin/payments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].paymentId", is(1)));
    }

    @Test
    void walletEndpoints_ReturnExpectedPayloads() throws Exception {
        when(walletService.addMoney(any())).thenReturn(new WalletBalanceResponse(1L, 11L, new BigDecimal("800.00"), LocalDateTime.now()));
        when(walletService.getStatements(11L)).thenReturn(List.of(
            new WalletStatementResponse(1L, 1L, 11L, new BigDecimal("50.00"), WalletTransactionType.CREDIT, "Top up", "TOPUP-1", LocalDateTime.now())
        ));

        mockMvc.perform(post("/api/v1/wallet/add-money")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerId":11,"amount":50.00,"description":"Top up"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.balance", is(800.00)));

        mockMvc.perform(get("/api/v1/wallet/statements/11"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].referenceId", is("TOPUP-1")));
    }

    @Test
    void webhookController_DelegatesToPaymentService() throws Exception {
        mockMvc.perform(post("/api/v1/payments/razorpay/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Razorpay-Signature", "signature")
                .content("{\"event\":\"payment.captured\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", is("Webhook processed successfully")));

        verify(paymentService).handleWebhookEvent("{\"event\":\"payment.captured\"}", "signature");
    }

    private PaymentResponse paymentResponse() {
        return new PaymentResponse(
            1L,
            101L,
            11L,
            new BigDecimal("250.00"),
            PaymentStatus.PAID,
            PaymentMode.CARD,
            "txn-1",
            "rzp_order_1",
            "rzp_payment_1",
            "INR",
            LocalDateTime.now(),
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}
