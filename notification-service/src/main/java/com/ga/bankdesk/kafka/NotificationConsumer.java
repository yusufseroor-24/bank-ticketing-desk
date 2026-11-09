package com.ga.bankdesk.kafka;

import com.ga.bankdesk.dto.EmailRequest;
import com.ga.bankdesk.dto.LiveNotificationRequest;
import com.ga.bankdesk.dto.TicketNotification;
import com.ga.bankdesk.notifications.SseEmitterRegistry;
import com.ga.bankdesk.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Receives the events ticket-service publishes and delivers it to the email or SSE
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationConsumer {

    private final EmailService emailService;
    private final SseEmitterRegistry registry;

    @KafkaListener(topics = "email-events", groupId = "notification-service",
            properties = "spring.json.value.default.type=com.ga.bankdesk.dto.EmailRequest")
    public void onEmail(EmailRequest r) {
        log.info("CONSUMER received email event '{}' for ticket {}", r.type(), r.ticketId());
        switch (r.type()) {
            case "TICKET_CREATED" -> emailService.sendTicketCreatedEmail(r.to(), r.ticketId(), r.title(), r.text());
            case "TICKET_STATUS_CHANGED" -> emailService.sendTicketStatusChangedEmail(r.to(), r.ticketId(), r.title(), r.text());
            case "SLA_ALERT" -> emailService.sendSlaAlertEmail(r.to(), r.ticketId(), r.title(), r.text(), r.dueAt());
            case "SLA_REPORT" -> emailService.sendSlaReportEmail(r.to(), r.text(), r.totalTickets(), r.compliance());
            default -> log.warn("Unknown email event type '{}', ignored", r.type());
        }
    }

    @KafkaListener(topics = "live-events", groupId = "notification-service",
            properties = "spring.json.value.default.type=com.ga.bankdesk.dto.LiveNotificationRequest")
    public void onLive(LiveNotificationRequest r) {
        log.info("CONSUMER received live event '{}' for ticket {}", r.eventType(), r.ticketId());
        TicketNotification n = new TicketNotification(r.ticketId(), r.title(), r.eventType(), r.message(), LocalDateTime.now());
        for (Long id : r.userIds()) {
            if (!id.equals(r.exceptUserId())) {
                registry.sendToUser(id, "ticket-update", n);
            }
        }
    }
}