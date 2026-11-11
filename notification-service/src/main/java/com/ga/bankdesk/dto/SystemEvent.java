package com.ga.bankdesk.dto;

import java.time.LocalDateTime;

//one Kafka event as shown in the admin "System events" tab
public record SystemEvent(String kind, LocalDateTime timestamp, String topic, String key,
                          String summary, String result) {

    public static SystemEvent of(LocalDateTime timestamp, String topic, String key, String summary, String result) {
        return new SystemEvent("system-event", timestamp, topic, key, summary, result);
    }
}