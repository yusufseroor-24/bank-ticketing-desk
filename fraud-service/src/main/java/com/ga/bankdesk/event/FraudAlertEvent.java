package com.ga.bankdesk.event;

import com.ga.bankdesk.enums.TicketPriority;

//published to the fraud-events topic when a fraud rule occurs, then ticket-service opens a ticket
public record FraudAlertEvent(String dedupKey, Long customerId, String ruleCode, String title, String description,
                              String categoryName, TicketPriority priority, String liveMessage) {
}