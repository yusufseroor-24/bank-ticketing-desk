package com.ga.bankdesk.cucumber;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.util.Date;

public final class TestTokens {

    private static final String SECRET = "cucumber-test-secret-that-is-long-enough-for-hs256-signing";

    private TestTokens() { }

    public static String token(long userId, String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("uid", userId)
                .claim("role", role)
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();
    }
}