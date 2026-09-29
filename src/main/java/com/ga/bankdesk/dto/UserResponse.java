package com.ga.bankdesk.dto;

import com.ga.bankdesk.model.Role;
import com.ga.bankdesk.model.UserStatus;

import java.time.LocalDateTime;

//what gets sent back to the user, no password and safe public data only
public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        UserStatus status,
        boolean emailVerified,
        LocalDateTime createdAt
) {
}
