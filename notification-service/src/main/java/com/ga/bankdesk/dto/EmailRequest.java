package com.ga.bankdesk.dto;

import java.time.LocalDateTime;

//what ticket-service sends to notification-service to ask for an email
//type: TICKET_CREATED, TICKET_STATUS_CHANGED, SLA_ALERT, SLA_REPORT
public record EmailRequest(String type, String to, Long ticketId, String title, String text,
                           LocalDateTime dueAt, Integer totalTickets, Double compliance) {
}
