package com.ga.bankdesk.dto;

import java.time.LocalDateTime;

public record HistoryResponse(
        String action,
        String changedByEmail,
        String oldValue,
        String newValue,
        LocalDateTime createdAt
) {
}
