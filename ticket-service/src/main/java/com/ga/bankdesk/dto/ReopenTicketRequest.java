package com.ga.bankdesk.dto;

import jakarta.validation.constraints.NotBlank;

public record ReopenTicketRequest(@NotBlank(message = "A reason is required to reopen a ticket") String reason) {
}
