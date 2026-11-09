package com.ga.bankdesk.service;

import com.ga.bankdesk.client.NotificationClient;
import com.ga.bankdesk.dto.EmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class EmailService {

    private final NotificationClient notificationClient;

    public void sendTicketCreatedEmail(String toEmail, Long ticketId, String title, String category) {
        notificationClient.email(new EmailRequest("TICKET_CREATED", toEmail, ticketId, title, category, null, null, null));
    }

    public void sendTicketStatusChangedEmail(String toEmail, Long ticketId, String title, String newStatus) {
        notificationClient.email(new EmailRequest("TICKET_STATUS_CHANGED", toEmail, ticketId, title, newStatus, null, null, null));
    }

    public void sendSlaAlertEmail(String toEmail, Long ticketId, String title, String headline, LocalDateTime dueAt) {
        notificationClient.email(new EmailRequest("SLA_ALERT", toEmail, ticketId, title, headline, dueAt, null, null));
    }

    public void sendSlaReportEmail(String toEmail, String period, Integer totalTickets, Double compliance) {
        notificationClient.email(new EmailRequest("SLA_REPORT", toEmail, null, null, period, null, totalTickets, compliance));
    }
}
