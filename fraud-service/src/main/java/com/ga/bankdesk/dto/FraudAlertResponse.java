package com.ga.bankdesk.dto;

import com.ga.bankdesk.model.FraudAlert;

import java.time.LocalDateTime;

public record FraudAlertResponse(Long id, String ruleCode, Long customerId, Long transactionId,
                                 Long ticketId, LocalDateTime createdAt) {
    public static FraudAlertResponse from(FraudAlert a) {
        return new FraudAlertResponse(a.getId(), a.getRule().getCode(), a.getSubjectUserId(),
                a.getTriggerTransaction() == null ? null : a.getTriggerTransaction().getId(),
                a.getCreatedTicketId(),
                a.getCreatedAt());
    }
}
