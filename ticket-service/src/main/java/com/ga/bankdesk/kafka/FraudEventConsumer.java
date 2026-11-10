package com.ga.bankdesk.kafka;

import com.ga.bankdesk.dto.SystemTicketRequest;
import com.ga.bankdesk.event.FraudAlertEvent;
import com.ga.bankdesk.model.ProcessedEvent;
import com.ga.bankdesk.repository.ProcessedEventRepository;
import com.ga.bankdesk.service.SystemTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

//opens the investigation ticket when fraud-service publishes a fraud alert
@Slf4j
@RequiredArgsConstructor
@Component
public class FraudEventConsumer {

    private final SystemTicketService systemTicketService;
    private final TicketEventProducer ticketEventProducer;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    @KafkaListener(topics = "fraud-events", groupId = "ticket-service",
            properties = "spring.json.value.default.type=com.ga.bankdesk.event.FraudAlertEvent")
    public void onFraudAlert(FraudAlertEvent e) {
        log.info("CONSUMER received fraud alert '{}' for customer {}", e.ruleCode(), e.customerId());
        String eventId = "fraud-alert:" + e.dedupKey();
        if (processedEventRepository.existsById(eventId)) {
            log.info("CONSUMER fraud alert {} was already processed, skipping", e.dedupKey());
            return;
        }

        Long ticketId = systemTicketService.create(new SystemTicketRequest(
                e.title(), e.description(), e.categoryName(), e.priority(), "FRAUD_ALERT", e.liveMessage(), null));
        processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));

        //tell fraud-service which ticket was opened, but only once this transaction is committed
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ticketEventProducer.fraudTicketOpened(e.dedupKey(), ticketId);
            }
        });
    }
}