package com.ga.bankdesk.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TriageRequest(@Size(max = 300) String title,
                            @Size(max = 5000) String description,
                            String currentCategory,
                            String currentPriority,
                            @Size(max = 50) List<CommentItem> comments,
                            @NotEmpty List<CategoryOption> categories) {
}