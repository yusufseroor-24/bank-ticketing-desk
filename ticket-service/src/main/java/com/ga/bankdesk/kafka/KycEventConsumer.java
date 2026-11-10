package com.ga.bankdesk.kafka;

import com.ga.bankdesk.dto.SystemTicketRequest;
import com.ga.bankdesk.enums.KycStatus;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.event.KycResultEvent;
import com.ga.bankdesk.repository.TicketRepository;
import com.ga.bankdesk.service.SystemTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * opens a KYC_REVIEW ticket for staff when identity-service publishes a KYC result
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class KycEventConsumer {

    private final SystemTicketService systemTicketService;
    private final TicketRepository ticketRepository;

    @KafkaListener(topics = "kyc-events", groupId = "ticket-service",
            properties = "spring.json.value.default.type=com.ga.bankdesk.event.KycResultEvent")
    public void onKycResult(KycResultEvent e) {
        log.info("CONSUMER received KYC result {} for user {} (session {})", e.status(), e.userId(), e.sessionId());
        if (e.status() == KycStatus.IN_REVIEW) {
            return;
        }
        //one ticket per KYC session
        String marker = "KYC session " + e.sessionId();
        if (ticketRepository.existsByDescriptionContaining(marker)) {
            log.info("KYC session {} already has a ticket, skipping", e.sessionId());
            return;
        }
        String title = "KYC " + e.status().name().replace('_', ' ') + " - " + e.fullName();
        String description = marker + "\n"
                + "Customer: " + e.fullName() + " (id " + e.userId() + ")\n"
                + "Result: " + e.status() + "\n"
                + "Please review the identity verification in the Didit console.";
        systemTicketService.create(new SystemTicketRequest(
                title, description, "KYC_REVIEW", TicketPriority.MEDIUM, "NEW_TICKET", "KYC review needed: " + title, e.userId()));
    }
}