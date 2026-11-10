package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.*;
import com.ga.bankdesk.security.CurrentUser;
import com.ga.bankdesk.service.AiAssistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/tickets")
public class AiController {

    private final AiAssistService aiAssistService;

    //draft of the creation ticket fields
    @PostMapping("/ai/draft")
    @PreAuthorize("hasRole('CUSTOMER')")
    public DraftResponse draft(@AuthenticationPrincipal CurrentUser currentUser,
                               @Valid @RequestBody DraftTicketRequest request) {
        return aiAssistService.draftTicket(currentUser, request);
    }

    //agent triage feature
    @PostMapping("/{ticketId}/ai/triage")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TriageResponse triage(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long ticketId) {
        return aiAssistService.triage(currentUser, ticketId);
    }

    //agent summary of ticket
    @PostMapping("/{ticketId}/ai/summary")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public SummaryResponse summary(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long ticketId) {
        return aiAssistService.summarize(currentUser, ticketId);
    }
}