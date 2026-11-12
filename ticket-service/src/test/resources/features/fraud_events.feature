Feature: Fraud alerts arriving over Kafka
  fraud-service publishes alerts, ticket-service opens an investigation ticket
  and answers back on Kafka. Delivery is at-least-once, so replays must be harmless.

  Scenario: A fraud alert opens one investigation ticket
    When fraud-service publishes a fraud alert for customer 1
    Then the system opens exactly one "FRAUD" ticket with priority "HIGH" for it
    And fraud-service is told which ticket was opened

  Scenario: The same alert delivered twice does not create a second ticket
    When fraud-service publishes a fraud alert for customer 1
    And the same alert is delivered again
    Then the system opens exactly one "FRAUD" ticket with priority "HIGH" for it