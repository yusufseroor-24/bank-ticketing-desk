package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.TicketPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CategoryRequest(String name,
                              boolean visibilityToCustomers) {
}
