package com.ga.bankdesk.cucumber;

import io.cucumber.spring.ScenarioScope;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@ScenarioScope
@Getter
@Setter
public class ScenarioState {
    private String token;                 // JWT of the user in this scenario
    private Response response;            // the last response we got back
    private Long ticketId;                // the ticket used in this scenario
    private String title;                 // title of the ticket / alert used in this scenario
    private String dedupKey;              // id of the fraud alert sent in this scenario
    private List<String> agentTokens = new ArrayList<>();   // agents taking part in a claim
    private List<Integer> claimStatuses = new ArrayList<>(); // result of each agent's claim

    public RequestSpecification request() {
        return requestWith(token);
    }

    public RequestSpecification requestWith(String jwt) {
        return RestAssured.given().header("Authorization", "Bearer " + jwt);
    }
}