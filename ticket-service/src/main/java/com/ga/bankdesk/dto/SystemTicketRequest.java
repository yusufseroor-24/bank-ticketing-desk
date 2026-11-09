package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

//asks ticket-service to open a ticket created by the system (e.g. a fraud alert from fraud-service)
public record SystemTicketRequest(@NotBlank String title, @NotBlank String description, @NotBlank String categoryName,
                                  @NotNull TicketPriority priority, @NotBlank String eventType, @NotBlank String liveMessage) {
}
