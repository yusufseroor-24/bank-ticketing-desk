package com.ga.bankdesk.event;

import com.ga.bankdesk.enums.KycStatus;

//published to the kyc-events topic when a customer's KYC result is saved
public record KycResultEvent(Long userId, String fullName, String sessionId, KycStatus status) {
}