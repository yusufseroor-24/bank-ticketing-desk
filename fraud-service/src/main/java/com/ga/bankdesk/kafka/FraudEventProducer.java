package com.ga.bankdesk.kafka;

import com.ga.bankdesk.event.FraudAlertEvent;
import com.ga.bankdesk.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Publishes a fraud alert for ticket-service
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class FraudEventProducer {

    public static final String TOPIC = "fraud-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(FraudAlertEvent event) {
        try {
            SendResult<String, Object> result = kafkaTemplate
                    .send(TOPIC, String.valueOf(event.customerId()), event)
                    .get(5, TimeUnit.SECONDS);
            log.info("PRODUCER sent fraud alert '{}' for customer {} to {} (partition {}, offset {})",
                    event.ruleCode(), event.customerId(), TOPIC,
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("PRODUCER could not send fraud alert '{}' to {}: {}", event.ruleCode(), TOPIC, e.getMessage());
            throw new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Could not publish the fraud alert event");
        }
    }
}