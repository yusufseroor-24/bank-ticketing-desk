package com.ga.bankdesk.service;

import com.ga.bankdesk.enums.KycStatus;
import com.ga.bankdesk.enums.SourceOfTicket;
import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.model.Category;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.notifications.LiveNotifier;
import com.ga.bankdesk.repository.CategoryRepository;
import com.ga.bankdesk.repository.TicketRepository;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Reacts to saved KYC result, audit log and email for customer
 * KYC_REVIEW Ticket for staff when result is DECLINED or IN_REVIEW
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class KycEventListener {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final SlaService slaService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final LiveNotifier liveNotifier;

    //runs only after the KYC status was committed
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onKycStatusChanged(KycStatusChangedEvent event) {
        KycStatus status = event.status();
        if (status != KycStatus.APPROVED && status != KycStatus.DECLINED && status != KycStatus.IN_REVIEW) {
            //pending/abandoned/expired need no action
            return;
        }
        try {
            User user = userRepository.findById(event.userId()).orElse(null);
            if (user == null) {
                return;
            }

            auditLogService.log(user, "KYC_" + status, "User", user.getId());
            emailService.sendKycResultEmail(user.getEmail(), status);

            if (status != KycStatus.APPROVED) {
                openReviewTicket(user, event);
            }
        } catch (Exception e) {
            log.error("KYC follow-up failed for session {}: {}", event.sessionId(), e.getMessage());
        }
    }

    private void openReviewTicket(User user, KycStatusChangedEvent event) {
        String marker = "KYC session " + event.sessionId();
        if (ticketRepository.existsByDescriptionContaining(marker)) {
            //one ticket per session
            return;
        }

        Category category = categoryRepository.findByName("KYC_REVIEW")
                .orElseThrow(() -> new IllegalStateException("Category not found: KYC_REVIEW"));

        Ticket ticket = new Ticket();
        ticket.setTitle("KYC " + event.status().name().replace('_', ' ') + " - " + user.getFullName());
        ticket.setDescription(marker + "\n"
                + "Customer: " + user.getFullName() + " (id " + user.getId() + ")\n"
                + "Result: " + event.status() + "\n"
                + "Please review the identity verification in the Didit console.");
        ticket.setCategory(category);
        ticket.setCustomer(user);
        //created by the system
        ticket.setCreatedBy(null);
        ticket.setSource(SourceOfTicket.SYSTEM);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(event.status() == KycStatus.IN_REVIEW ? TicketPriority.MEDIUM : TicketPriority.LOW);
        slaService.applyDeadlines(ticket, LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);
        liveNotifier.send(liveNotifier.staffFor(saved), null, saved.getId(), saved.getTitle(),
                "NEW_TICKET", "KYC review needed: " + saved.getTitle());
        log.info("Opened KYC_REVIEW ticket #{} for user {} (session {})", saved.getId(), user.getId(), event.sessionId());
    }
}
