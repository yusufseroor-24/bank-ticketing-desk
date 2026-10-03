package com.ga.bankdesk.dto;

import jakarta.validation.constraints.Min;

public record CategoryRequest(String name,
                              @Min(value = 1, message = "SLA hours must be at least 1") int slaHours,
                              boolean visibilityToCustomers) {
}
