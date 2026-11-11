package com.ga.bankdesk.kafka;

import com.ga.bankdesk.dto.SystemEvent;
import com.ga.bankdesk.notifications.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Watches every BankDesk topic with its own consumer group, keeps the last 100 events in memory and pushes each new one to the connected admins.
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class SystemEventMonitor {

    private static final int MAX_EVENTS = 100;

    private final SseEmitterRegistry registry;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Deque<SystemEvent> recent = new ArrayDeque<>();

    @KafkaListener(topicPattern = "(email|live|fraud|ticket|kyc)-events(\\.DLT)?",
            groupId = "notification-monitor",
            properties = {
                    "key.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"})
    public void onEvent(ConsumerRecord<String, String> record) {
        try {
            SystemEvent event = toEvent(record);
            synchronized (this) {
                recent.addFirst(event);
                while (recent.size() > MAX_EVENTS) {
                    recent.removeLast();
                }
            }
            registry.sendToAdmins("system-event", event);
        } catch (Exception e) {
            log.warn("Monitor could not handle a record from {}: {}", record.topic(), e.getMessage());
        }
    }

    //newest first
    public synchronized List<SystemEvent> recent() {
        return List.copyOf(recent);
    }

    private SystemEvent toEvent(ConsumerRecord<String, String> record) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault());
        String topic = record.topic();
        if (topic.endsWith(".DLT")) {
            return SystemEvent.of(time, topic, record.key(),
                    "Could not be processed after 5 retries, parked for inspection (from " + topic.replace(".DLT", "") + ")", "DLT");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = mapper.readValue(record.value(), Map.class);
            return SystemEvent.of(time, topic, record.key(), describe(topic, m), "OK");
        } catch (Exception e) {
            return SystemEvent.of(time, topic, record.key(), "Unreadable message", "UNREADABLE");
        }
    }

    //a short sentence per topic, without personal data (no emails, no names)
    private String describe(String topic, Map<String, Object> m) {
        return switch (topic) {
            case "email-events" -> "Email " + m.get("type") + " for ticket #" + m.get("ticketId");
            case "live-events" -> "Live notification " + m.get("eventType")
                    + (m.get("ticketId") != null ? " for ticket #" + m.get("ticketId") : "");
            case "fraud-events" -> "Fraud rule " + m.get("ruleCode") + " matched for customer "
                    + m.get("customerId") + ", ticket requested";
            case "ticket-events" -> "Ticket #" + m.get("ticketId") + " opened for the fraud alert";
            case "kyc-events" -> "Identity verification " + m.get("status") + " for user " + m.get("userId");
            default -> topic;
        };
    }
}