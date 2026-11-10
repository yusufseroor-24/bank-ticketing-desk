package com.ga.bankdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DraftTicketRequest(@NotBlank(message = "Please write a few words about your problem")
                                 @Size(max = 4000, message = "Text is too long (max 4000 characters)") String text,
                                 @Size(max = 200, message = "Title is too long (max 200 characters)") String title){
}
