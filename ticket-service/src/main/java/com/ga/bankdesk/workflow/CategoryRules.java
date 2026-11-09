package com.ga.bankdesk.workflow;

import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import com.ga.bankdesk.model.Ticket;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-category logic
 */

@Component
public class CategoryRules {

    private static final Set<TicketStatus> NOTE_REQUIRED_STATUSES = EnumSet.of(TicketStatus.ESCALATED, TicketStatus.RESOLVED);

    //status that needs a note in specific categories (to explain the decision)
    private final Map<String, Set<TicketStatus>> extraStatusNoteRequired = Map.of(
            "FRAUD", EnumSet.of(TicketStatus.CONFIRMED, TicketStatus.FALSE_POSITIVE),
            "KYC_REVIEW",EnumSet.of(TicketStatus.APPROVED, TicketStatus.REJECTED),
            "CARD_DISPUTE", EnumSet.of(TicketStatus.APPROVED, TicketStatus.REJECTED)
    );

    //lowest possible priority a category can have
    private final Map<String, TicketPriority> minimumPriority = Map.of(
            "AML", TicketPriority.HIGH,
            "FRAUD", TicketPriority.HIGH,
            "IT_SECURITY", TicketPriority.HIGH
    );

    public boolean isNoteRequired(String category, TicketStatus to){
        return NOTE_REQUIRED_STATUSES.contains(to) || extraStatusNoteRequired.getOrDefault(category, Set.of()).contains(to);
    }

    public TicketPriority minimumPriority(String category){
        return minimumPriority.getOrDefault(category, TicketPriority.LOW);
    }
}
