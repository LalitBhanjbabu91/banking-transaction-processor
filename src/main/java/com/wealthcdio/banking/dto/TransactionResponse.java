package com.wealthcdio.banking.dto;

import com.wealthcdio.banking.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        Long accountId,
        TransactionType type,
        BigDecimal amount,
        Instant timestamp
) {
}
