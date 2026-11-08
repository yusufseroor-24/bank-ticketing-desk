package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.KycStartResponse;
import com.ga.bankdesk.dto.KycStatusResponse;
import com.ga.bankdesk.security.AppUserDetails;
import com.ga.bankdesk.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/kyc")
public class KycController {

    private final KycService kycService;

    @PostMapping("/start")
    public KycStartResponse start(@AuthenticationPrincipal AppUserDetails userDetails) {
        return kycService.start(userDetails.getUsername());
    }

    @GetMapping("/status")
    public KycStatusResponse status(@AuthenticationPrincipal AppUserDetails userDetails) {
        return kycService.status(userDetails.getUsername());
    }
}
