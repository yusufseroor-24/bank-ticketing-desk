package com.ga.bankdesk.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record SummaryRequest(@Size(max = 300) String title,
                             @Size(max = 5000) String description,
                             String status,
                             String category,
                             String priority,
                             @Size(max = 50) List<CommentItem> comments) {
}