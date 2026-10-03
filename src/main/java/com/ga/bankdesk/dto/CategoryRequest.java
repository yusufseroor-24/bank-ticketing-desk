package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.TicketPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(String name,
                              @Min(value = 1, message = "SLA hours must be at least 1") int slaHours,
                              @NotNull(message = "Default priority is required")TicketPriority defaultPriority,
                              boolean visibilityToCustomers) {
}
