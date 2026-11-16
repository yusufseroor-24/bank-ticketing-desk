package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.TicketStatus;

import java.time.LocalDateTime;

public record DeletedTicketResponse(Long id, String title, TicketStatus status, LocalDateTime deletedAt) {
}