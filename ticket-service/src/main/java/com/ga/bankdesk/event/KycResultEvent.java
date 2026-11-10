package com.ga.bankdesk.event;

import com.ga.bankdesk.enums.KycStatus;

public record KycResultEvent(Long userId, String fullName, String sessionId, KycStatus status) {
}