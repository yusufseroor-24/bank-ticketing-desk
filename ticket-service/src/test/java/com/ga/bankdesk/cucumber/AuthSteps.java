package com.ga.bankdesk.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthSteps {
    private final ScenarioState state;

    public AuthSteps(ScenarioState state) {
        this.state = state;
    }

    @Given("a customer whose identity is verified")
    public void verifiedCustomer() {
        state.setToken(TestTokens.token(1, "verified@bankdesk.test", "CUSTOMER"));
    }

    @Given("a customer whose identity is not verified")
    public void unverifiedCustomer() {
        state.setToken(TestTokens.token(2, "unverified@bankdesk.test", "CUSTOMER"));
    }

    @When("someone lists tickets without a token")
    public void listWithoutToken() {
        state.setResponse(given().when().get("/api/tickets/my-tickets"));
    }

    @Then("the request is rejected as unauthenticated")
    public void rejected() {
        int status = state.getResponse().statusCode();
        assertTrue(status == 401 || status == 403, "Expected 401 or 403 but got " + status);
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) {
        assertEquals(expectedStatus, state.getResponse().statusCode(),
                "Response body: " + state.getResponse().asString());
    }

    @Then("the error message mentions {string}")
    public void theErrorMessageMentions(String text) {
        String message = state.getResponse().jsonPath().getString("message");
        assertTrue(message != null && message.toLowerCase().contains(text.toLowerCase()),
                "Expected the message to mention '" + text + "' but got: " + message);
    }
}