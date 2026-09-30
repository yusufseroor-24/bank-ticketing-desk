package com.ga.bankdesk.dto;

public record LoginResponse(String token, String email, String role) {
}
