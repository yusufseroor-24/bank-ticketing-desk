package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull(message = "Role is required") Role newRole) {
}
