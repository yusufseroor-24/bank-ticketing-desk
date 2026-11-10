package com.ga.bankdesk.kafka;

import com.ga.bankdesk.dto.SystemTicketRequest;
import com.ga.bankdesk.event.FraudAlertEvent;
import com.ga.bankdesk.service.SystemTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * opens the investigation ticket when fraud-service publishes a fraud alert
 */

@Slf4j
@RequiredArgsConstructor
@Component
public class FraudEventConsumer{

    private final SystemTicketService systemTicketService;
    private final TicketEventProducer ticketEventProducer;

    @KafkaListener(topics = "fraud-events", groupId = "ticket-service",
            properties = "spring.json.value.default.type=com.ga.bankdesk.event.FraudAlertEvent")
    public void onFraudAlert(FraudAlertEvent e) {
        log.info("CONSUMER received fraud alert '{}' for customer {}", e.ruleCode(), e.customerId());
        Long ticketId = systemTicketService.create(new SystemTicketRequest(
                e.title(), e.description(), e.categoryName(), e.priority(), "FRAUD_ALERT", e.liveMessage(), null));
        ticketEventProducer.fraudTicketOpened(e.dedupKey(), ticketId);
    }
}