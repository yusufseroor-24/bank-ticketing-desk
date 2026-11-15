package com.ga.bankdesk.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

//read-only view of the email thread pool, used by the Concurrency simulation
@RestController
@RequestMapping("/api/admin/diagnostics")
public class DiagnosticsController {

    private final ThreadPoolTaskExecutor executor;

    public DiagnosticsController(@Qualifier("notificationExecutor") ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    @GetMapping("/email")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> email() {
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        List<Map<String, String>> threads = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().startsWith("notification-"))
                .sorted(Comparator.comparing(Thread::getName))
                .map(t -> Map.of("name", t.getName(), "state", t.getState().name()))
                .toList();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("corePoolSize", pool.getCorePoolSize());
        info.put("active", pool.getActiveCount());
        info.put("queued", pool.getQueue().size());
        info.put("queueCapacity", executor.getQueueCapacity());
        info.put("completed", pool.getCompletedTaskCount());
        info.put("threads", threads);
        return Map.of("notification", info);
    }
}