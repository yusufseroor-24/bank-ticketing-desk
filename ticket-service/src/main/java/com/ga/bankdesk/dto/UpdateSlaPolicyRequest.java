package com.ga.bankdesk.dto;

import jakarta.validation.constraints.Min;

public record UpdateSlaPolicyRequest(
        @Min(value = 1, message = "First response time must be at least 1 minutes") int firstResponseMinutes,
        @Min(value = 1, message = "Resolution time must be at least 1 minute") int resolutionMinutes) {
}
