package com.ga.bankdesk.service;

import com.ga.bankdesk.kafka.NotificationProducer;
import com.ga.bankdesk.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class EmailService {

    private final NotificationProducer notificationProducer;

    public void sendTicketCreatedEmail(String toEmail, Long ticketId, String title, String category) {
        notificationProducer.email(new EmailRequest("TICKET_CREATED", toEmail, ticketId, title, category, null, null, null));
    }

    public void sendTicketStatusChangedEmail(String toEmail, Long ticketId, String title, String newStatus) {
        notificationProducer.email(new EmailRequest("TICKET_STATUS_CHANGED", toEmail, ticketId, title, newStatus, null, null, null));
    }

    public void sendSlaAlertEmail(String toEmail, Long ticketId, String title, String headline, LocalDateTime dueAt) {
        notificationProducer.email(new EmailRequest("SLA_ALERT", toEmail, ticketId, title, headline, dueAt, null, null));
    }

    public void sendSlaReportEmail(String toEmail, String period, Integer totalTickets, Double compliance) {
        notificationProducer.email(new EmailRequest("SLA_REPORT", toEmail, null, null, period, null, totalTickets, compliance));
    }
}
