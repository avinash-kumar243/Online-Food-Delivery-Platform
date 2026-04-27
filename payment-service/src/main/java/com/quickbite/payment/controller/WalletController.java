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
import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.WalletBalanceResponse;
import com.quickbite.payment.dto.WalletPaymentRequest;
import com.quickbite.payment.dto.WalletStatementResponse;
import com.quickbite.payment.dto.WalletTopUpRequest;
import com.quickbite.payment.service.WalletService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/add-money")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> addMoney(@Valid @RequestBody WalletTopUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Money added to wallet successfully", walletService.addMoney(request)));
    }

    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> payFromWallet(@Valid @RequestBody WalletPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Wallet payment completed successfully", walletService.payFromWallet(request)));
    }

    @GetMapping("/balance/{customerId}")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getBalance(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success("Wallet balance fetched successfully", walletService.getBalance(customerId)));
    }

    @GetMapping("/statements/{customerId}")
    public ResponseEntity<ApiResponse<List<WalletStatementResponse>>> getStatements(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success("Wallet statements fetched successfully", walletService.getStatements(customerId)));
    }
}
