package com.ga.bankdesk.cucumber;

import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

//starts the real ticket-service on port 8092 using the "cucumber" profile(H2) and an in-memory Kafka
@CucumberContextConfiguration
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"spring.profiles.active=cucumber", "server.port=8092"}
)
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers",
        topics = {"fraud-events", "kyc-events"})
@Import(ScenarioState.class)
public class CucumberSpringConfiguration {

    //the fake identity-service starts on a free port and ticket-service is pointed at it
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("identity.service.url", FakeIdentityService::start);
    }

    //runs before every scenario
    @Before
    public void setUpRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8092;
        FakeIdentityService.down = false;
    }
}