package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTicketStatusRequest(@NotNull(message = "New status is required")TicketStatus newStatus) {
}
