package com.ga.bankdesk.workflow;

import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.enums.TicketStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryRulesTest {
    private final CategoryRules categoryRules = new CategoryRules();

    @Test
    void fraudDecisionsNeedANote() {
        assertTrue(categoryRules.isNoteRequired("FRAUD", TicketStatus.CONFIRMED));
        assertTrue(categoryRules.isNoteRequired("FRAUD", TicketStatus.FALSE_POSITIVE));
    }

    @Test
    void escalatingAlwaysNeedsANote() {
        assertTrue(categoryRules.isNoteRequired("AML", TicketStatus.ESCALATED));
        assertTrue(categoryRules.isNoteRequired("COMPLAINT", TicketStatus.RESOLVED));
    }

    @Test
    void kycAndCardDisputeDecisionsNeedANote() {
        assertTrue(categoryRules.isNoteRequired("KYC_REVIEW", TicketStatus.REJECTED));
        assertTrue(categoryRules.isNoteRequired("CARD_DISPUTE", TicketStatus.APPROVED));
    }

    @Test
    void normalTransitionsDontNeedANote() {
        assertFalse(categoryRules.isNoteRequired("COMPLAINT", TicketStatus.IN_PROGRESS));
        assertFalse(categoryRules.isNoteRequired("CARD_DISPUTE", TicketStatus.CLOSED));
    }

    @Test
    void securityCategoriesHaveHighMinimumPriority() {
        assertEquals(TicketPriority.HIGH, categoryRules.minimumPriority("AML"));
        assertEquals(TicketPriority.HIGH, categoryRules.minimumPriority("IT_SECURITY"));
        assertEquals(TicketPriority.LOW, categoryRules.minimumPriority("COMPLAINT"));
    }

}