package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.SourceOfTicket;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;

import java.time.LocalDateTime;

public record TicketCreationResponse(
        Long id,
        String title,
        String description,
        String categoryName,
        SourceOfTicket source,
        String customerEmail,
        String createdByEmail,
        String assignedToEmail,
        TicketPriority priority,
        TicketStatus status,
        LocalDateTime dueAt,
        LocalDateTime createdAt
) {
}
