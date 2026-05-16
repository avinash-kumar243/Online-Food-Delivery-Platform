package com.quickbite.payment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.payment.dto.ApiResponse;
import com.quickbite.payment.dto.CodPaymentRequest;
import com.quickbite.payment.dto.CreatePaymentOrderRequest;
import com.quickbite.payment.dto.CreatePaymentOrderResponse;
import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.RefundRequest;
import com.quickbite.payment.dto.VerifyPaymentRequest;
import com.quickbite.payment.enums.PaymentStatus;
import com.quickbite.payment.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/razorpay/create-order")
    public ResponseEntity<ApiResponse<CreatePaymentOrderResponse>> createRazorpayOrder(
        @Valid @RequestBody CreatePaymentOrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Razorpay order created successfully", paymentService.createRazorpayOrder(request)));
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyRazorpayPayment(
        @Valid @RequestBody VerifyPaymentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", paymentService.verifyRazorpayPayment(request)));
    }

    @PostMapping("/cod")
    public ResponseEntity<ApiResponse<PaymentResponse>> createCodPayment(
        @Valid @RequestBody CodPaymentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("COD payment created successfully", paymentService.createCodPayment(request)));
    }

    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(@Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Refund request processed successfully", paymentService.refundPayment(request)));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Payment fetched successfully", paymentService.getPaymentByOrderId(orderId)));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success("Customer payments fetched successfully", paymentService.getPaymentsByCustomerId(customerId)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully", paymentService.getPaymentsByStatus(status)));
    }
}
