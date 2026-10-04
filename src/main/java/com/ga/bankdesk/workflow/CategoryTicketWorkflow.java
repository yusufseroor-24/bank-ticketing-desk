package com.ga.bankdesk.workflow;

import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.model.Ticket;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


//rulebook for how different ticket categories states are allowed to move
@Component
public class CategoryTicketWorkflow {

    //Object... pairs (accepts any num of arg and puts it in an array called pairs (object allows diff types in the array)
    //helper so the maps read (status, allowed next option, status, allowed next option)
    private Map<TicketStatus, Set<TicketStatus>> buildMap(Object... pairs) {
        Map<TicketStatus, Set<TicketStatus>> map = new EnumMap<>(TicketStatus.class);
        for (int i=0; i < pairs.length; i += 2) {
            map.put(
                    (TicketStatus) pairs[i],
                    (Set<TicketStatus>) pairs[i + 1]);
        }
        return map;
    }

    //one category workflow: Map<TicketStatus, Set<TicketStatus>> : current status --> allowed next statuses
    private final Map<String, Map<TicketStatus, Set<TicketStatus>>> transitionByCategory = Map.of(
            "AML", buildMap(
                    TicketStatus.OPEN,
                    EnumSet.of(TicketStatus.ASSIGNED),
                    TicketStatus.ASSIGNED,
                    EnumSet.of(TicketStatus.UNDER_REVIEW),
                    TicketStatus.UNDER_REVIEW,
                    EnumSet.of(TicketStatus.ESCALATED, TicketStatus.RESOLVED),
                    TicketStatus.ESCALATED,
                    EnumSet.of(TicketStatus.RESOLVED),
                    TicketStatus.RESOLVED,
                    EnumSet.of(TicketStatus.CLOSED)
            ),
            "FRAUD", buildMap(
                    TicketStatus.OPEN,
                    EnumSet.of(TicketStatus.ASSIGNED),
                    TicketStatus.ASSIGNED,
                    EnumSet.of(TicketStatus.INVESTIGATING),
                    TicketStatus.INVESTIGATING,
                    EnumSet.of(TicketStatus.CONFIRMED, TicketStatus.FALSE_POSITIVE),
                    TicketStatus.CONFIRMED,
                    EnumSet.of(TicketStatus.CLOSED),
                    TicketStatus.FALSE_POSITIVE,
                    EnumSet.of(TicketStatus.CLOSED)
            ),
            "KYC_REVIEW", buildMap(
                    TicketStatus.OPEN,
                    EnumSet.of(TicketStatus.ASSIGNED),
                    TicketStatus.ASSIGNED,
                    EnumSet.of(TicketStatus.UNDER_REVIEW),
                    TicketStatus.UNDER_REVIEW,
                    EnumSet.of(TicketStatus.APPROVED, TicketStatus.REJECTED),
                    TicketStatus.APPROVED,
                    EnumSet.of(TicketStatus.CLOSED),
                    TicketStatus.REJECTED,
                    EnumSet.of(TicketStatus.CLOSED)
            ),
            "CARD_DISPUTE", buildMap(
                    TicketStatus.OPEN,
                    EnumSet.of(TicketStatus.ASSIGNED),
                    TicketStatus.ASSIGNED,
                    EnumSet.of(TicketStatus.UNDER_REVIEW),
                    TicketStatus.UNDER_REVIEW,
                    EnumSet.of(TicketStatus.APPROVED, TicketStatus.REJECTED),
                    TicketStatus.APPROVED,
                    EnumSet.of(TicketStatus.CLOSED),
                    TicketStatus.REJECTED,
                    EnumSet.of(TicketStatus.CLOSED)
            ),
            "IT_SECURITY", buildMap(
                    TicketStatus.OPEN,
                    EnumSet.of(TicketStatus.ASSIGNED),
                    TicketStatus.ASSIGNED,
                    EnumSet.of(TicketStatus.IN_PROGRESS),
                    TicketStatus.IN_PROGRESS,
                    EnumSet.of(TicketStatus.RESOLVED),
                    TicketStatus.RESOLVED,
                    EnumSet.of(TicketStatus.CLOSED)
            ),
            "LOAN_ACCOUNT", buildMap(
                    TicketStatus.OPEN,
                    EnumSet.of(TicketStatus.ASSIGNED),
                    TicketStatus.ASSIGNED,
                    EnumSet.of(TicketStatus.IN_PROGRESS),
                    TicketStatus.IN_PROGRESS,
                    EnumSet.of(TicketStatus.RESOLVED),
                    TicketStatus.RESOLVED,
                    EnumSet.of(TicketStatus.CLOSED)
            ),
            "COMPLAINT", buildMap(
                    TicketStatus.OPEN,
                    EnumSet.of(TicketStatus.ASSIGNED),
                    TicketStatus.ASSIGNED,
                    EnumSet.of(TicketStatus.IN_PROGRESS),
                    TicketStatus.IN_PROGRESS,
                    EnumSet.of(TicketStatus.RESOLVED),
                    TicketStatus.RESOLVED,
                    EnumSet.of(TicketStatus.CLOSED)
            )
    );

    //is the transition allowed?
    public boolean isValidTransition(String category, TicketStatus from, TicketStatus to){
        Map<TicketStatus, Set<TicketStatus>> transitions = transitionByCategory.get(category);
        if(transitions == null){
            return false; //unknown category
        }
        Set<TicketStatus> allowedNextStatus = transitions.get(from);
        return allowedNextStatus != null && allowedNextStatus.contains(to);
    }

    //what transition is allowed?
    public Set<TicketStatus> getAllowedNextStatus(String category, TicketStatus from){
        Map<TicketStatus, Set<TicketStatus>> transitions = transitionByCategory.get(category);
        if(transitions == null){
            return Set.of();
        }
        return transitions.getOrDefault(from, Set.of());

    }

}
