package com.quickbite.payment.service;

import java.math.BigDecimal;
import java.util.List;

import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.WalletBalanceResponse;
import com.quickbite.payment.dto.WalletPaymentRequest;
import com.quickbite.payment.dto.WalletStatementResponse;
import com.quickbite.payment.dto.WalletTopUpRequest;

public interface WalletService {

    WalletBalanceResponse addMoney(WalletTopUpRequest request);

    PaymentResponse payFromWallet(WalletPaymentRequest request);

    WalletBalanceResponse getBalance(Long customerId);

    List<WalletStatementResponse> getStatements(Long customerId);

    void creditRefund(Long customerId, BigDecimal amount, String description, String referenceId);
}
