package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.EmailRequest;
import com.ga.bankdesk.dto.LiveNotificationRequest;
import com.ga.bankdesk.dto.TicketNotification;
import com.ga.bankdesk.notifications.SseEmitterRegistry;
import com.ga.bankdesk.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * service-to-service only (needs the X-Internal-Key header), called by ticket-service.
 * This service only delivers.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

    private final EmailService emailService;
    private final SseEmitterRegistry registry;

    @PostMapping("/email")
    public ResponseEntity<Void> email(@RequestBody EmailRequest r) {
        switch (r.type()) {
            case "TICKET_CREATED" -> emailService.sendTicketCreatedEmail(r.to(), r.ticketId(), r.title(), r.text());
            case "TICKET_STATUS_CHANGED" -> emailService.sendTicketStatusChangedEmail(r.to(), r.ticketId(), r.title(), r.text());
            case "SLA_ALERT" -> emailService.sendSlaAlertEmail(r.to(), r.ticketId(), r.title(), r.text(), r.dueAt());
            case "SLA_REPORT" -> emailService.sendSlaReportEmail(r.to(), r.text(), r.totalTickets(), r.compliance());
            default -> {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.accepted().build();
    }

    //Sends the live (SSE) event to every id except exceptUserId
    @PostMapping("/live")
    public ResponseEntity<Void> live(@RequestBody LiveNotificationRequest r) {
        TicketNotification n = new TicketNotification(r.ticketId(), r.title(), r.eventType(), r.message(), LocalDateTime.now());
        for (Long id : r.userIds()) {
            if (!id.equals(r.exceptUserId())) {
                registry.sendToUser(id, "ticket-update", n);
            }
        }
        return ResponseEntity.accepted().build();
    }
}
