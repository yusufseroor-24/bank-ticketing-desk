package com.ga.bankdesk.kafka;

import com.ga.bankdesk.event.FraudTicketOpenedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TicketEventProducer {

    public static final String TOPIC = "ticket-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void fraudTicketOpened(String dedupKey, Long ticketId) {
        kafkaTemplate.send(TOPIC, dedupKey, new FraudTicketOpenedEvent(dedupKey, ticketId))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("PRODUCER could not send ticket #{} for alert {} to {}: {}", ticketId, dedupKey, TOPIC, ex.getMessage());
                    } else {
                        log.info("PRODUCER sent ticket #{} for alert {} to {} (partition {}, offset {})", ticketId, dedupKey, TOPIC,
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}