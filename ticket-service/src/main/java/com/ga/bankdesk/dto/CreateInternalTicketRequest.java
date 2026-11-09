package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInternalTicketRequest(@NotBlank(message = "Title of ticket is required") String title,
                                          @NotBlank(message = "Description of ticket is required") String description,
                                          @NotNull(message = "Category of ticket is required") Long categoryId,
                                          @NotNull TicketPriority priority,
                                          Long customerId ) {  //optional
}
