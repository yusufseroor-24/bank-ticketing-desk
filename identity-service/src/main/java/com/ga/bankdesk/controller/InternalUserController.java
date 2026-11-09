package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.UserSummary;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * service-to-service only (needs the X-Internal-Key header): other services ask identity-service about users.
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public UserSummary getUser(@PathVariable Long id) {
        return userRepository.findById(id).map(this::toSummary)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + id + " was not found"));
    }

    @GetMapping("/by-email")
    public UserSummary getByEmail(@RequestParam String email) {
        return userRepository.findByEmail(email).map(this::toSummary)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " was not found"));
    }

    @GetMapping
    public List<UserSummary> list(@RequestParam(required = false) String ids, @RequestParam(required = false) Role role) {
        List<User> users;
        if (ids != null && !ids.isBlank()) {
            List<Long> idList = Arrays.stream(ids.split(",")).map(String::trim).map(Long::valueOf).toList();
            users = userRepository.findAllById(idList);
        } else if (role != null) {
            users = userRepository.findByRole(role);
        } else {
            users = List.of();
        }
        return users.stream().map(this::toSummary).toList();
    }

    private UserSummary toSummary(User u) {
        return new UserSummary(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getStatus(), u.getKycStatus());
    }
}
