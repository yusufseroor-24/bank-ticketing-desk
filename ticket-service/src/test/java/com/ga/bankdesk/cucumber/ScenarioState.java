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
    private String token;
    private Response response;
    private Long ticketId;
    private String title;
    //id of the fraud alert sent in this scenario
    private String dedupKey;
    //agents taking part in a claim
    private List<String> agentTokens = new ArrayList<>();
    //result of each agent's claim
    private List<Integer> claimStatuses = new ArrayList<>();

    public RequestSpecification request() {
        return requestWith(token);
    }

    public RequestSpecification requestWith(String jwt) {
        return RestAssured.given().header("Authorization", "Bearer " + jwt);
    }
}