package com.ga.bankdesk.service;

import com.ga.bankdesk.enums.KycStatus;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to a saved KYC result: audit log + email to the customer.
 * The "KYC_REVIEW ticket" part lives in ticket-service and will be triggered by a Kafka event later.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class KycEventListener {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

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
        } catch (Exception e) {
            log.error("KYC follow-up failed for session {}: {}", event.sessionId(), e.getMessage());
        }
    }
}
