package com.ga.bankdesk.kafka;

import com.ga.bankdesk.dto.EmailRequest;
import com.ga.bankdesk.dto.LiveNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Publishes notification requests to Kafka, notification-service consumes them and does the delivery.
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationProducer {

    public static final String EMAIL_TOPIC = "email-events";
    public static final String LIVE_TOPIC = "live-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async("notificationExecutor")
    public void email(EmailRequest request) {
        send(EMAIL_TOPIC, request.ticketId(), request, request.type());
    }

    @Async("notificationExecutor")
    public void live(LiveNotificationRequest request) {
        send(LIVE_TOPIC, request.ticketId(), request, request.eventType());
    }

    private void send(String topic, Long ticketId, Object event, String what) {
        String key = ticketId == null ? null : String.valueOf(ticketId);
        try {
            kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("PRODUCER could not send '{}' to {}: {}", what, topic, ex.getMessage());
                } else {
                    log.info("PRODUCER sent '{}' to {} (partition {}, offset {})", what, topic,
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("PRODUCER could not send '{}' to {}: {}", what, topic, e.getMessage());
        }
    }
}