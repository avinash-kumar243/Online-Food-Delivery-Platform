package com.quickbite.payment.service;

import java.util.List;

import com.quickbite.payment.dto.CodPaymentRequest;
import com.quickbite.payment.dto.CreatePaymentOrderRequest;
import com.quickbite.payment.dto.CreatePaymentOrderResponse;
import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.RefundRequest;
import com.quickbite.payment.dto.VerifyPaymentRequest;
import com.quickbite.payment.enums.PaymentStatus;

public interface PaymentService {

    CreatePaymentOrderResponse createRazorpayOrder(CreatePaymentOrderRequest request);

    PaymentResponse verifyRazorpayPayment(VerifyPaymentRequest request);

    PaymentResponse createCodPayment(CodPaymentRequest request);

    PaymentResponse refundPayment(RefundRequest request);

    PaymentResponse getPaymentByOrderId(Long orderId);

    List<PaymentResponse> getPaymentsByCustomerId(Long customerId);

    List<PaymentResponse> getPaymentsByStatus(PaymentStatus status);

    List<PaymentResponse> getAllPayments();

    void handleWebhookEvent(String payload, String signature);
}
