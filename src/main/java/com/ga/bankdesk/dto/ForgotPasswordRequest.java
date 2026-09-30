package com.ga.bankdesk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank(message = "Email is requried")
                                    @Email(message = "Email must be a valid address") String email) {
}
