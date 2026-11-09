package com.ga.bankdesk.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks whether SLA levels are calculated correctly
 * test levelFor() method in SlaNotificationService
 */

class SlaNotificationServiceTest {

    private final LocalDateTime created = LocalDateTime.of(2026, 1, 1, 0, 0);
    private final LocalDateTime due = created.plusHours(100);

    @Test
    void levels(){
        assertEquals(0, SlaNotificationService.levelFor(created, due, created.plusHours(50)));
        assertEquals(1, SlaNotificationService.levelFor(created, due, created.plusHours(75)));
        assertEquals(1, SlaNotificationService.levelFor(created, due, created.plusHours(89)));
        assertEquals(2, SlaNotificationService.levelFor(created, due, created.plusHours(90)));
        assertEquals(3, SlaNotificationService.levelFor(created, due, due));
        assertEquals(3, SlaNotificationService.levelFor(created, due, due.plusHours(20)));
    }

}