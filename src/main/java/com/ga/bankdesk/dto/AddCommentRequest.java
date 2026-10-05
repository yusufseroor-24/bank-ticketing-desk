package com.ga.bankdesk.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(@NotBlank(message = "Comment content is required") String commentContent) {
}
