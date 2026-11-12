package com.ga.bankdesk.cucumber;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public final class ApiCalls {

    private ApiCalls() { }

    public static Response createTicket(String token, String title, long categoryId, String priority) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {"title": "%s", "description": "created by a cucumber scenario", "categoryId": %d, "priority": "%s"}"""
                        .formatted(title, categoryId, priority))
                .when()
                .post("/api/tickets/create");
    }
}