package com.ga.bankdesk.controller;

import com.ga.bankdesk.notifications.SseEmitterRegistry;
import com.ga.bankdesk.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SseEmitterRegistry emitterRegistry;

    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe(@AuthenticationPrincipal AppUserDetails userDetails){
        return emitterRegistry.register(userDetails.getUser().getId());
    }
}
