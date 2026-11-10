package com.ga.bankdesk.service;

import com.ga.bankdesk.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Max AI calls per user per hour (in memory, so it resets when the service restarts)
 */

@Component
public class AiRateLimiter {

    private final int maxCallsPerHour;
    private final Map<Long, Deque<Instant>> calls = new ConcurrentHashMap<>();

    public AiRateLimiter(@Value("${app.ai.max-calls-per-hour:20}") int maxCallsPerHour) {
        this.maxCallsPerHour = maxCallsPerHour;
    }

    public void check(Long userId) {
        Deque<Instant> queue = calls.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (queue) {
            Instant now = Instant.now();
            Instant cutoff = now.minus(Duration.ofHours(1));
            while (!queue.isEmpty() && queue.peekFirst().isBefore(cutoff)) {
                queue.pollFirst();
            }
            if (queue.size() >= maxCallsPerHour) {
                throw new ExternalServiceException(HttpStatus.TOO_MANY_REQUESTS,
                        "You reached the limit of " + maxCallsPerHour + " AI requests per hour, please try again later");
            }
            queue.addLast(now);
        }
    }
}