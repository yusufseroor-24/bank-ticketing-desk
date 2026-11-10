package com.ga.bankdesk.dto;

import java.util.List;

public record TriageCall(String title, String description, String currentCategory, String currentPriority,
                         List<CommentItem> comments, List<CategoryOption> categories) {
}
