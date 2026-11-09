package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.KycStartResponse;
import com.ga.bankdesk.dto.KycStatusResponse;
import com.ga.bankdesk.enums.KycStatus;
import com.ga.bankdesk.exception.ConflictException;
import com.ga.bankdesk.exception.ExternalServiceException;
import com.ga.bankdesk.exception.ResourceNotFoundException;
import com.ga.bankdesk.model.KycVerification;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.KycVerificationRepository;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Starts verification, checks user kYC status, and update user KYC status when Didit sends a result
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class KycService {

    private final DiditClient diditClient;
    private final KycVerificationRepository kycRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    //starts a new KYC verification session
    public KycStartResponse start(String email) {
        User user = findUser(email);
        KycStatus current = currentStatus(user);

        if(current == KycStatus.APPROVED) {
            throw new ConflictException("Identity is already verified");
        }
        if(current == KycStatus.IN_REVIEW) {
            throw new ConflictException("Your verification is under review");
        }
        if(!diditClient.isConfigured()) {
            throw new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Identity verification is not configured");
        }

        //creating Didit session
        DiditClient.DiditSession session;
        try {
            //send user ID to Didit, creates a verification session and receives the session ID and verification URL
            session = diditClient.createSession(user.getId());
        } catch (RuntimeException e) {
            log.error("Didit session creation failed for user {}", user.getId(), e);
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, "Identity verification provider is unavailable");
        }

        //create verification record
        KycVerification verification = new KycVerification();
        verification.setUserId(user.getId());
        verification.setSessionId(session.sessionId());
        kycRepository.save(verification);

        user.setKycStatus(KycStatus.PENDING);
        userRepository.save(user);

        log.info("KYC session {} started for user {}", session.sessionId(), user.getId());
        return new KycStartResponse(session.sessionId(), session.url());
    }

    //returns the user current KYC status
    public KycStatusResponse status(String email) {
        User user = findUser(email);
        return new KycStatusResponse(currentStatus(user), user.getKycVerifiedAt());
    }

    //Called by the webhook, safe to call twice with the same data
    //processes the verification result received from Didit
    @Transactional
    public void handleStatusUpdate(String sessionId, String diditStatus) {
        //convert Didit Status
        KycStatus newStatus = KycStatus.fromDidit(diditStatus);
        if (newStatus == null) {
            log.warn("Ignoring KYC webhook with unknown status '{}'", diditStatus);
            return;
        }

        //finding the verification record
        KycVerification verification = kycRepository.findBySessionIdForUpdate(sessionId).orElse(null);
        if (verification == null) {
            log.warn("Ignoring KYC webhook for unknown session {}", sessionId);
            return;
        }

        //duplicate status from didit or a late event after a final decision
        if (verification.getStatus() == newStatus || verification.getStatus().isFinal()) {
            return;
        }

        verification.setStatus(newStatus);
        kycRepository.save(verification);

        //checking whether this is the user latest verification session
        boolean isLatest = kycRepository.findFirstByUserIdOrderByCreatedAtDesc(verification.getUserId())
                .map(latest -> latest.getId().equals(verification.getId()))
                .orElse(false);
        if (!isLatest) {
            return;
        }

        User user = userRepository.findById(verification.getUserId()).orElse(null);
        if (user == null) {
            return;
        }
        user.setKycStatus(newStatus);
        if (newStatus == KycStatus.APPROVED) {
            user.setKycVerifiedAt(Instant.now());
        }
        userRepository.save(user);

        log.info("KYC session {} for user {} is now {}", sessionId, user.getId(), newStatus);
        eventPublisher.publishEvent(new KycStatusChangedEvent(user.getId(), sessionId, newStatus));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private KycStatus currentStatus(User user) {
        return user.getKycStatus() == null ? KycStatus.NOT_STARTED : user.getKycStatus();
    }
}
