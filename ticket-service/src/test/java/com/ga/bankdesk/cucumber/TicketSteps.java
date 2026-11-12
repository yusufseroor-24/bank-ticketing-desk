package com.ga.bankdesk.cucumber;

import com.ga.bankdesk.repository.CategoryRepository;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Steps for ticket creation and Kafka publishing
 */

public class TicketSteps {
    private final ScenarioState state;
    private final KafkaProbe probe;

    @Autowired private CategoryRepository categoryRepository;

    public TicketSteps(ScenarioState state, KafkaProbe probe) {
        this.state = state;
        this.probe = probe;
    }

    @When("the customer opens a {string} ticket with priority {string}")
    public void customerOpensTicket(String category, String priority) {
        long categoryId = categoryRepository.findByName(category).orElseThrow().getId();
        state.setTitle("Cucumber ticket " + UUID.randomUUID());
        Response response = ApiCalls.createTicket(state.getToken(), state.getTitle(), categoryId, priority);
        state.setResponse(response);
        if (response.statusCode() == 201) {
            state.setTicketId(response.jsonPath().getLong("id"));
        }
    }

    @Then("the ticket status is {string}")
    public void ticketStatus(String expected) {
        assertEquals(expected, state.getResponse().jsonPath().getString("status"));
    }

    @Then("the ticket priority is {string}")
    public void ticketPriority(String expected) {
        assertEquals(expected, state.getResponse().jsonPath().getString("priority"));
    }

    @Then("an email event for the ticket is published to Kafka")
    public void emailPublished() {
        String marker = "\"ticketId\":" + state.getTicketId();
        await().atMost(Duration.ofSeconds(20))
                .until(() -> probe.emailEvents.stream()
                        .anyMatch(m -> m.contains(marker)));
    }

    @Then("a live notification for the ticket is published to Kafka")
    public void livePublished() {
        String marker = "\"ticketId\":" + state.getTicketId();
        await().atMost(Duration.ofSeconds(20))
                .until(() -> probe.liveEvents.stream()
                        .anyMatch(m -> m.contains(marker)));
    }
}