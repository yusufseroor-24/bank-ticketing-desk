package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.TransactionType;
import com.ga.bankdesk.model.BankTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(Long id, Long customerId, String accountRef, TransactionType type,
                                  BigDecimal amount, String currency, String country, LocalDateTime occurredAt) {

    //converts BankTransactions into TransactionResponse
    public static TransactionResponse from(BankTransaction t) {
        return new TransactionResponse(t.getId(), t.getCustomer().getId(), t.getAccountRef(), t.getType(),
                t.getAmount(), t.getCurrency(), t.getCountry(), t.getOccurredAt());
    }
}
