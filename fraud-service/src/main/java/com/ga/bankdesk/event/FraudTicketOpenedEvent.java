package com.ga.bankdesk.event;

public record FraudTicketOpenedEvent(String dedupKey, Long ticketId) {
}