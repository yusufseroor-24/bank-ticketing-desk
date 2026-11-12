package com.ga.bankdesk.cucumber;

import com.ga.bankdesk.enums.TicketPriority;
import com.ga.bankdesk.event.FraudAlertEvent;
import com.ga.bankdesk.model.Ticket;
import com.ga.bankdesk.repository.TicketRepository;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fraud steps, Kafka, and idempotency
 */

public class FraudSteps {
    private final ScenarioState state;
    private final KafkaProbe probe;

    @Autowired private KafkaTemplate<String, Object> kafka;
    @Autowired private TicketRepository ticketRepository;

    public FraudSteps(ScenarioState state, KafkaProbe probe) {
        this.state = state;
        this.probe = probe;
    }

    @When("fraud-service publishes a fraud alert for customer {int}")
    public void fraudAlert(int customerId) {
        state.setDedupKey("cucumber-" + UUID.randomUUID());
        state.setTitle("Fraud alert " + state.getDedupKey());
        publishAlert(state.getDedupKey(), state.getTitle(), customerId);
    }

    @When("the same alert is delivered again")
    public void sameAlertAgain() {
        publishAlert(state.getDedupKey(), state.getTitle(), 1);
        String markerKey = state.getDedupKey() + "-marker";
        String markerTitle = "Marker " + markerKey;
        publishAlert(markerKey, markerTitle, 1);
        await().atMost(Duration.ofSeconds(20)).until(() -> ticketsTitled(markerTitle) == 1);
    }

    @Then("the system opens exactly one {string} ticket with priority {string} for it")
    public void oneSystemTicket(String category, String priority) {
        String title = state.getTitle();
        await().atMost(Duration.ofSeconds(20)).until(() -> ticketsTitled(title) >= 1);
        List<Ticket> found = ticketRepository.findAll().stream()
                .filter(t -> title.equals(t.getTitle()))
                .toList();
        assertEquals(1, found.size(), "Tickets found: " + found.size());
        assertEquals(category, found.get(0).getCategory().getName());
        assertEquals(priority, found.get(0).getPriority().name());
        assertEquals("SYSTEM", found.get(0).getSource().name());
    }

    @Then("fraud-service is told which ticket was opened")
    public void fraudToldTicket() {
        String dedupKey = state.getDedupKey();
        await().atMost(Duration.ofSeconds(20))
                .until(() -> probe.ticketEvents.stream()
                        .anyMatch(m -> m.contains(dedupKey)));
    }

    private void publishAlert(String key, String title, long customerId) {
        kafka.send("fraud-events", key, new FraudAlertEvent(key, customerId, "CUCUMBER_RULE", title,
                "sent by a cucumber scenario", "FRAUD", TicketPriority.HIGH, "Cucumber fraud alert"));
    }

    private long ticketsTitled(String wanted) {
        return ticketRepository.findAll().stream().filter(t -> wanted.equals(t.getTitle())).count();
    }
}