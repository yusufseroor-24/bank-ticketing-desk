package com.ga.bankdesk.dto;

import jakarta.validation.constraints.NotNull;

public record EscalateTicketRequest(@NotNull(message = "A note is required to escalate a ticket") String note) {
}
