package com.quickbite.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletBalanceResponse(
    Long walletId,
    Long customerId,
    BigDecimal balance,
    LocalDateTime updatedAt
) {
}
