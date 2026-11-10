package com.ga.bankdesk.kafka;

import com.ga.bankdesk.event.KycResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Publishes a KYC result for ticket-service.
 * Runs after the KYC status is already committed, so a failure is logged and does not undo the KYC result.
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class KycEventProducer {

    public static final String TOPIC = "kyc-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(KycResultEvent event) {
        try {
            SendResult<String, Object> result = kafkaTemplate
                    .send(TOPIC, String.valueOf(event.userId()), event)
                    .get(5, TimeUnit.SECONDS);
            log.info("PRODUCER sent KYC result {} for user {} to {} (partition {}, offset {})",
                    event.status(), event.userId(), TOPIC,
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("PRODUCER could not send KYC result {} for user {} to {}: {}", event.status(), event.userId(), TOPIC, e.getMessage());
        }
    }
}