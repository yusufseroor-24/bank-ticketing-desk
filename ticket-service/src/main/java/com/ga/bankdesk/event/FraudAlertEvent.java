package com.ga.bankdesk.event;

import com.ga.bankdesk.enums.TicketPriority;

public record FraudAlertEvent(String dedupKey, Long customerId, String ruleCode, String title, String description,
                              String categoryName, TicketPriority priority, String liveMessage) {
}