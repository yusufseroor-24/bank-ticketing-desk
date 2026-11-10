package com.ga.bankdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DraftRequest(@NotBlank @Size(max = 4000) String text,
                           @Size(max = 200) String title,
                           @NotEmpty List<CategoryOption> categories) {
}