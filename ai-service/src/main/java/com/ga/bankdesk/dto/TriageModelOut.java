package com.ga.bankdesk.dto;

public record TriageModelOut(Long categoryId, String priority, String confidence, String reason) {
}