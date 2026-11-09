package com.ga.bankdesk.workflow;

import com.ga.bankdesk.enums.TicketStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTicketWorkflowTest {

    private final CategoryTicketWorkflow workflow = new CategoryTicketWorkflow();

    @Test
    void allowsOpenToAssignedForCardDispute(){
        assertTrue(workflow.isValidTransition("CARD_DISPUTE", TicketStatus.OPEN, TicketStatus.ASSIGNED));
    }

    @Test
    void rejectSkippingStraightToClosedStatus(){
        assertFalse(workflow.isValidTransition("CARD_DISPUTE", TicketStatus.OPEN, TicketStatus.CLOSED));
    }

    @Test
    void amlCanEscalateFromUnderReviewStatus(){
        assertTrue(workflow.isValidTransition("AML", TicketStatus.UNDER_REVIEW, TicketStatus.ESCALATED));
    }

    @Test
    void cardDisputeHasNoEscalationStatus(){
        assertFalse(workflow.isValidTransition("CARD_DISPUTE", TicketStatus.UNDER_REVIEW, TicketStatus.ESCALATED));
    }

}