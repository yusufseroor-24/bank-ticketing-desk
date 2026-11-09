package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.KycStatus;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.enums.UserStatus;

//what identity-service tells other services about a user (copy of identity's /internal/users response)
public record UserSummary(Long id, String email, String fullName, Role role, UserStatus status, KycStatus kycStatus) {
}
