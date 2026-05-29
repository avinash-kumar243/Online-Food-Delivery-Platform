package com.quickbite.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.quickbite.payment.enums.WalletTransactionType;

public record WalletStatementResponse(
    Long statementId,
    Long walletId,
    Long customerId,
    BigDecimal amount,
    WalletTransactionType transactionType,
    String description,
    String referenceId,
    LocalDateTime createdAt
) {
}
