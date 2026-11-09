package com.ga.bankdesk.notifications;

import com.ga.bankdesk.client.IdentityClient;
import com.ga.bankdesk.client.NotificationClient;
import com.ga.bankdesk.dto.LiveNotificationRequest;
import com.ga.bankdesk.dto.UserSummary;
import com.ga.bankdesk.enums.Role;
import com.ga.bankdesk.model.AgentCategory;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.repository.AgentCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Decides who receives a live event
 * the SSE push itself is done by notification-service, called after the transaction commits.
 */

@RequiredArgsConstructor
@Component
public class LiveNotifier {

    private final IdentityClient identityClient;
    private final NotificationClient notificationClient;
    private final AgentCategoryRepository agentCategoryRepository;

    //the assigned agent or everyone assigned to category
    public Set<Long> staffFor(Ticket ticket) {
        Set<Long> ids = new LinkedHashSet<>();
        if (ticket.getAssignedToId() != null) {
            ids.add(ticket.getAssignedToId());
        } else {
            for (AgentCategory ac : agentCategoryRepository.findByCategoryId(ticket.getCategory().getId())) {
                ids.add(ac.getAgentId());
            }
        }
        return ids;
    }

    public Set<Long> admins() {
        Set<Long> ids = new LinkedHashSet<>();
        for (UserSummary admin : identityClient.getUsersByRole(Role.ADMIN)) {
            ids.add(admin.id());
        }
        return ids;
    }

    //Sends to every id except exceptUserId
    public void send(Set<Long> userIds, Long exceptUserId, Long ticketId, String title, String eventType, String message) {
        LiveNotificationRequest request = new LiveNotificationRequest(userIds, exceptUserId, ticketId, title, eventType, message);
        afterCommit(() -> notificationClient.live(request));
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
