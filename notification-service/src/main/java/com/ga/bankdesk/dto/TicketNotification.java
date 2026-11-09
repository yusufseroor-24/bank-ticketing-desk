package com.ga.bankdesk.dto;

import java.time.LocalDateTime;

public record TicketNotification(Long ticketId, String title,
                                 String eventType, String message,
                                 LocalDateTime timestamp) {
}
