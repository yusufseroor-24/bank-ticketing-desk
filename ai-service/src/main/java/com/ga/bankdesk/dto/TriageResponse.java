package com.ga.bankdesk.dto;

public record TriageResponse(Long categoryId, String categoryName, String priority,
                             String confidence, String reason) {
}