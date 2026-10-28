package com.ga.bankdesk.service;

import com.ga.bankdesk.dto.TicketNotification;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.model.AgentCategory;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.notifications.SseEmitterRegistry;
import com.ga.bankdesk.repository.AgentCategoryRepository;
import com.ga.bankdesk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Background SLA scheduler (Every 60s)
 * levelFor: 0 = fine, 1 = 75% , 2 = 90% , 3 = deadline passed
 * time used = time elapsed / total available time
 * A new level is saved with an atomic UPDATE
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class SlaNotificationService {

    private final TicketRepository ticketRepository;
    private final AgentCategoryRepository agentCategoryRepository;
    private final SseEmitterRegistry emitterRegistry;
    private final EmailService emailService;

    @Scheduled(initialDelay = 30000, fixedDelayString = "${app.sla.check-interval-ms:60000}")
    public void checkTickets(){
        long start = System.currentTimeMillis();
        int notified = 0;
        LocalDateTime now = LocalDateTime.now();

        List<Ticket> candidates = ticketRepository.findSlaCandidates(TicketStatus.CLOSED);

        for(Ticket ticket : candidates){
            try{
                int level = levelFor(ticket.getCreatedAt(), ticket.getDueAt(), now);
                //compare calculated level with previous level
                if(level <= ticket.getSlaNotifyLevel()) {
                    //nothing new so continue
                    continue;
                }
                //atomic update to prevent duplicate alerts
                if(ticketRepository.raiseSlaNotifyLevel(ticket.getId(), level) == 0){
                    log.debug("SLA level {} for ticket {} already handled elsewhere", level, ticket.getId());
                    continue;
                }
                notifyAgents(ticket, level);
                notified++;
            }catch (Exception e){
                log.error("SLA check failed for ticket {}: {}", ticket.getId(), e.getMessage());
            }
        }
        if(notified > 0){
            log.info("SLA check: {} alerts raise in {} ms", notified, System.currentTimeMillis() - start);
        }
    }

    //calculates current SLA notification level
    static int levelFor(LocalDateTime createdAt, LocalDateTime dueAt, LocalDateTime now) {
        //check if the deadline passed
        if (!now.isBefore(dueAt)) {
            return 3;
        }
        //calculates the total available time
        long total = Duration.between(createdAt, dueAt).toSeconds();

        if (total <= 0) {
            return 0;
        }
        //calculates the percentage time used
        double used = (double) Duration.between(createdAt, now).toSeconds() / total;

        if (used >= 0.90) return 2;
        if (used >= 0.75) return 1;
        return 0;
    }

    //sending notifications to agents
    private void notifyAgents(Ticket ticket, int level) {
        String text = switch (level) {
            case 1 -> "SLA warning: 75% of the resolution time is used";
            case 2 -> "SLA urgent: 90% of the resolution time is used";
            default -> "SLA BREACHED: the resolution deadline has passed";
        };
        String eventType = (level == 3)
                ? "SLA_BREACHED"
                : "SLA_WARNING";

        //finding the recipients
        for (User agent : recipients(ticket)) {
            //send live notification to agents
            emitterRegistry.sendToUser(agent.getId(), "ticket-update",
                    new TicketNotification(ticket.getId(), ticket.getTitle(), eventType, text, LocalDateTime.now()));
            //send email notification to agents
            emailService.sendSlaAlertEmail(agent.getEmail(), ticket.getId(), ticket.getTitle(), text, ticket.getDueAt());
        }
        log.warn("SLA level {} reached for ticket {} ({})", level, ticket.getId(), ticket.getCategory().getName());
    }

    //who should be notified about the ticket
    private List<User> recipients(Ticket ticket) {
        //the ticket assigned agent
        if (ticket.getAssignedTo() != null) {
            return List.of(ticket.getAssignedTo());
        }
        //nobody is assigned, find agents with the same ticket category and send them the alerts
        return agentCategoryRepository.findByCategoryId(ticket.getCategory().getId()).stream()
                .map(AgentCategory::getAgent)
                .toList();
    }
}
