package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.KycStatus;

import java.time.Instant;

public record KycStatusResponse(KycStatus status, Instant verifiedAt) {
}
