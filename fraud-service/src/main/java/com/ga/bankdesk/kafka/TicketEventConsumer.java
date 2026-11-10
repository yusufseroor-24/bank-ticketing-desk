package com.ga.bankdesk.kafka;

import com.ga.bankdesk.event.FraudTicketOpenedEvent;
import com.ga.bankdesk.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

//saves the link between a fraud alert and the ticket that ticket-service opened for it
@Slf4j
@RequiredArgsConstructor
@Component
public class TicketEventConsumer {

    private final FraudAlertRepository alertRepository;

    @Transactional
    @KafkaListener(topics = "ticket-events", groupId = "fraud-service",
            properties = "spring.json.value.default.type=com.ga.bankdesk.event.FraudTicketOpenedEvent")
    public void onTicketOpened(FraudTicketOpenedEvent e) {
        if (alertRepository.attachTicket(e.dedupKey(), e.ticketId()) == 0) {
            throw new IllegalStateException("Alert " + e.dedupKey() + " is not saved yet");
        }
        log.info("CONSUMER linked alert {} to ticket #{}", e.dedupKey(), e.ticketId());
    }
}