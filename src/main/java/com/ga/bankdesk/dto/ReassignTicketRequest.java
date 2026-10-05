package com.ga.bankdesk.dto;

import jakarta.validation.constraints.NotNull;

public record ReassignTicketRequest(@NotNull(message = "Agent ID is required") Long agentId) {
}
