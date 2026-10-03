package com.ga.bankdesk.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AssignCategoriesRequest(@NotEmpty(message = "At least one category must be assigned") List<Long> categoryIds) {
}
