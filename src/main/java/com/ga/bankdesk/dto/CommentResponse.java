package com.ga.bankdesk.dto;

import java.time.LocalDateTime;

public record CommentResponse(Long id, String authorEmail, String commentContent, LocalDateTime createdAt) {
}
