package com.ga.bankdesk.security;

import com.ga.bankdesk.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

//the logged-in user, built from the JWT claims (replaces the User entity / AppUserDetails of the monolith)
@Getter
@AllArgsConstructor
public class CurrentUser {
    private final Long id;
    private final String email;
    private final Role role;
}
