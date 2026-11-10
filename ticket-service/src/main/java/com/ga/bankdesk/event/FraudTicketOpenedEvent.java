package com.ga.bankdesk.event;

//published to ticket-events after the ticket of a fraud alert is opened, fraud-service links the alert to it
public record FraudTicketOpenedEvent(String dedupKey, Long ticketId) {
}