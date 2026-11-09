package com.ga.bankdesk.service;

import com.ga.bankdesk.enums.KycStatus;

//published after KYC result is saved
public record KycStatusChangedEvent(Long userId, String sessionId, KycStatus status){
}
