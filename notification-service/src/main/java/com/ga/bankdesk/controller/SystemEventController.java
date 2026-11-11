package com.ga.bankdesk.controller;

import com.ga.bankdesk.dto.SystemEvent;
import com.ga.bankdesk.kafka.SystemEventMonitor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/system-events")
@PreAuthorize("hasRole('ADMIN')")
public class SystemEventController {

    private final SystemEventMonitor monitor;

    //the last 100 Kafka events, newest first
    @GetMapping
    public List<SystemEvent> recent() {
        return monitor.recent();
    }
}