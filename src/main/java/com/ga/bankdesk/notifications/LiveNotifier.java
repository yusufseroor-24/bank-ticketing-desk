package com.ga.bankdesk.notifications;

import com.ga.bankdesk.dto.TicketNotification;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.model.AgentCategory;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.AgentCategoryRepository;
import com.ga.bankdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * one place to push SSE live events to multiple users
 * Decides who receives a live event
 * Sends notification over SSE after transaction commits
 */

@RequiredArgsConstructor
@Component
public class LiveNotifier {

    private final UserRepository userRepository;
    private final SseEmitterRegistry registry;
    private final AgentCategoryRepository agentCategoryRepository;


    //the assigned agent or everyone assigned to category
    public Set<Long> staffFor(Ticket ticket) {
        Set<Long> ids = new LinkedHashSet<>();
        if (ticket.getAssignedTo() != null) {
            ids.add(ticket.getAssignedTo().getId());
        } else {
            for (AgentCategory ac : agentCategoryRepository.findByCategoryId(ticket.getCategory().getId())) {
                ids.add(ac.getAgent().getId());
            }
        }
        return ids;
    }

    public Set<Long> admins() {
        Set<Long> ids = new LinkedHashSet<>();
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            ids.add(admin.getId());
        }
        return ids;
    }

    //Sends to every id
    public void send(Set<Long> userIds, Long exceptUserId, Long ticketId, String title, String eventType, String message) {
        TicketNotification n = new TicketNotification(ticketId, title, eventType, message, LocalDateTime.now());
        afterCommit(() -> {
            for (Long id : userIds) {
                if (!id.equals(exceptUserId)) {
                    registry.sendToUser(id, "ticket-update", n);
                }
            }
        });
    }

    //If a database transaction is running, wait until it is committed
    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
