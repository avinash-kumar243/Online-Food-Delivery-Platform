package com.quickbite.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.payment.dto.ApiResponse;
import com.quickbite.payment.service.PaymentService;

@RestController
@RequestMapping("/api/v1/payments/razorpay/webhook")
public class RazorpayWebhookController {

    private final PaymentService paymentService;

    public RazorpayWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("X-Razorpay-Signature") String signature
    ) {
        paymentService.handleWebhookEvent(payload, signature);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully", null));
    }
}
