package com.ga.bankdesk.service;

import com.ga.bankdesk.enums.KycStatus;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.UserRepository;
import com.ga.bankdesk.event.KycResultEvent;
import com.ga.bankdesk.kafka.KycEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to a saved KYC result and does an audit log + email to the customer.
 * publishes kyc-event for ticket-service
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class KycEventListener {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final KycEventProducer kycEventProducer;

    //runs only after the KYC status was committed
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onKycStatusChanged(KycStatusChangedEvent event) {
        KycStatus status = event.status();
        if (status != KycStatus.APPROVED && status != KycStatus.DECLINED && status != KycStatus.IN_REVIEW) {
            return; //pending/abandoned/expired need no action
        }
        try {
            User user = userRepository.findById(event.userId()).orElse(null);
            if (user == null) {
                return;
            }
            auditLogService.log(user, "KYC_" + status, "User", user.getId());
            emailService.sendKycResultEmail(user.getEmail(), status);
            kycEventProducer.publish(new KycResultEvent(user.getId(), user.getFullName(), event.sessionId(), status));
        } catch (Exception e) {
            log.error("KYC follow-up failed for session {}: {}", event.sessionId(), e.getMessage());
        }
    }
}
