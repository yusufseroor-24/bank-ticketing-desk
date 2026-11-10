package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.*;
import com.ga.bankdesk.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/internal/ai")
public class InternalAiController {

    private final AiService aiService;

    @PostMapping("/draft-ticket")
    public DraftResponse draft(@Valid @RequestBody DraftRequest request) {
        return aiService.draftTicket(request);
    }

    @PostMapping("/triage")
    public TriageResponse triage(@Valid @RequestBody TriageRequest request) {
        return aiService.triage(request);
    }

    @PostMapping("/summarize")
    public SummaryResponse summarize(@Valid @RequestBody SummaryRequest request) {
        return aiService.summarize(request);
    }
}