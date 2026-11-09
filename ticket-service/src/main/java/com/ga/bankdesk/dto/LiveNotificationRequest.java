package com.ga.bankdesk.dto;

import java.util.Set;

//what ticket-service sends to notification-service to push a live (SSE) message to users
public record LiveNotificationRequest(Set<Long> userIds, Long exceptUserId, Long ticketId, String title,
                                      String eventType, String message) {
}
