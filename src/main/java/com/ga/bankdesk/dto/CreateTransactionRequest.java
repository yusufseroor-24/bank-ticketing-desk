package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateTransactionRequest(
        @NotNull(message = "Customer ID is required") Long customerId,
        @NotBlank(message = "Account reference is required") String accountRef,
        @NotNull(message = "Transaction type is required") TransactionType type,
        @NotNull @Positive(message = "Amount must be positive") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3, message = "Currency must be a 3-letter code") String currency,
        @NotBlank @Size(min = 2, max = 2, message = "Country must be a 2-letter code") String country,
        LocalDateTime occurredAt
) {
}
