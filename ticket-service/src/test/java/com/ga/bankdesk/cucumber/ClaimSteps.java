package com.ga.bankdesk.cucumber;

import com.ga.bankdesk.model.AgentCategory;
import com.ga.bankdesk.repository.AgentCategoryRepository;
import com.ga.bankdesk.repository.CategoryRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps to test concurrent ticket claim behavior
 */

public class ClaimSteps {
    private final ScenarioState state;

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private AgentCategoryRepository agentCategoryRepository;

    public ClaimSteps(ScenarioState state) {
        this.state = state;
    }

    @Given("{int} agents who can handle {string} tickets")
    public void agentsWhoCanHandle(int count, String category) {
        for (int i = 1; i <= count; i++) {
            long agentId = 100 + i;
            allowAgent(agentId, category);
            state.getAgentTokens().add(TestTokens.token(agentId, "agent" + agentId + "@bankdesk.test", "AGENT"));
        }
    }

    @Given("an agent who only handles {string} tickets")
    public void agentWhoOnlyHandles(String category) {
        allowAgent(201, category);
        state.getAgentTokens().add(TestTokens.token(201, "agent201@bankdesk.test", "AGENT"));
    }

    @Given("an open {string} ticket")
    public void anOpenTicket(String category) {
        long categoryId = categoryRepository.findByName(category).orElseThrow().getId();
        String customer = TestTokens.token(1, "verified@bankdesk.test", "CUSTOMER");
        Response created = ApiCalls.createTicket(customer, "Cucumber ticket " + UUID.randomUUID(), categoryId, "MEDIUM");
        assertEquals(201, created.statusCode(), "Could not create the ticket: " + created.asString());
        state.setTicketId(created.jsonPath().getLong("id"));
    }

    @When("all agents try to claim it at the same moment")
    public void agentsRace() throws Exception {
        List<String> agents = state.getAgentTokens();
        Long ticketId = state.getTicketId();
        ExecutorService pool = Executors.newFixedThreadPool(agents.size());
        CountDownLatch ready = new CountDownLatch(agents.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (String agentToken : agents) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return claim(agentToken, ticketId).statusCode();
            }));
        }
        ready.await();
        //release every agent at the same moment
        go.countDown();
        for (Future<Integer> f : results) {
            state.getClaimStatuses().add(f.get());
        }
        pool.shutdown();
    }

    @When("the agent tries to claim the ticket")
    public void agentClaims() {
        state.setResponse(claim(state.getAgentTokens().get(0), state.getTicketId()));
    }

    @Then("exactly {int} agent wins the ticket")
    public void exactlyWinners(int winners) {
        long won = state.getClaimStatuses().stream().filter(s -> s == 200).count();
        assertEquals(winners, won, "Statuses: " + state.getClaimStatuses());
    }

    @Then("every other agent gets {int}")
    public void everyOtherGets(int expected) {
        List<Integer> losers = state.getClaimStatuses().stream().filter(s -> s != 200).toList();
        assertFalse(losers.isEmpty(), "Nobody lost the race");
        assertTrue(losers.stream().allMatch(s -> s == expected), "Statuses: " + state.getClaimStatuses());
    }

    private Response claim(String agentToken, Long ticketId) {
        return RestAssured.given().header("Authorization", "Bearer " + agentToken)
                .when().put("/api/tickets/" + ticketId + "/claim");
    }

    private void allowAgent(long agentId, String category) {
        var cat = categoryRepository.findByName(category).orElseThrow();
        if (!agentCategoryRepository.existsByAgentIdAndCategoryId(agentId, cat.getId())) {
            AgentCategory link = new AgentCategory();
            link.setAgentId(agentId);
            link.setCategory(cat);
            agentCategoryRepository.save(link);
        }
    }
}