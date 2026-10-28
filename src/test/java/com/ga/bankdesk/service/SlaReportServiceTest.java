package com.ga.bankdesk.service;

import com.ga.bankdesk.model.Ticket;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlaReportServiceTest {

    private final LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);

    private Ticket ticket(LocalDateTime resolvedAt, LocalDateTime dueAt, LocalDateTime firstRespondedAt, LocalDateTime firstResponseDueAt) {
        Ticket t = new Ticket();
        t.setCreatedAt(start);
        t.setResolvedAt(resolvedAt);
        t.setDueAt(dueAt);
        t.setFirstRespondedAt(firstRespondedAt);
        t.setFirstResponseDueAt(firstResponseDueAt);
        return t;
    }

    @Test
    void countsMetBreachedAndInProgress() {
        LocalDateTime now = start.plusHours(20);
        List<Ticket> tickets = List.of(
                //resolved in time, first response in time
                ticket(start.plusHours(5), start.plusHours(10), start.plusMinutes(30), start.plusHours(1)),
                //resolved late, first response late
                ticket(start.plusHours(12), start.plusHours(10), start.plusHours(2), start.plusHours(1)),
                //still open and past both deadlines
                ticket(null, start.plusHours(10), null, start.plusHours(1)),
                //still open, deadlines not reached yet
                ticket(null, start.plusHours(100), null, start.plusHours(50)));

        SlaReportService.Stats s = SlaReportService.stats(tickets, now);

        assertEquals(4, s.created());
        assertEquals(2, s.resolved());
        assertEquals(1, s.resolutionMet());
        assertEquals(2, s.resolutionBreached());
        assertEquals(1, s.firstResponseMet());
        assertEquals(2, s.firstResponseBreached());
        assertEquals(8.5, s.avgResolutionHours(), 0.001);

        assertEquals(33.3, SlaReportService.percent(s.resolutionMet(), s.resolutionBreached()), 0.05);
        assertNull(SlaReportService.percent(0, 0));
    }

}