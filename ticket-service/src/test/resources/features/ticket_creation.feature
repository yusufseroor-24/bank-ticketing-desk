Feature: Opening a ticket
  Customers open tickets, but only after their identity (KYC) is verified.
  Opening a ticket also publishes events to Kafka for notification-service.

  Scenario: A verified customer opens a ticket
    Given a customer whose identity is verified
    When the customer opens a "CARD_DISPUTE" ticket with priority "MEDIUM"
    Then the response status is 201
    And the ticket status is "OPEN"

  Scenario: Opening a ticket publishes email and live notification events
    Given a customer whose identity is verified
    When the customer opens a "COMPLAINT" ticket with priority "LOW"
    Then the response status is 201
    And an email event for the ticket is published to Kafka
    And a live notification for the ticket is published to Kafka

  Scenario: A customer who is not verified cannot open a normal ticket
    Given a customer whose identity is not verified
    When the customer opens a "CARD_DISPUTE" ticket with priority "MEDIUM"
    Then the response status is 422
    And the error message mentions "verify your identity"

  Scenario: A customer who is not verified can still open a KYC review ticket
    Given a customer whose identity is not verified
    When the customer opens a "KYC_REVIEW" ticket with priority "MEDIUM"
    Then the response status is 201

  Scenario: Sensitive categories raise the priority to the minimum
    Given a customer whose identity is verified
    When the customer opens a "IT_SECURITY" ticket with priority "LOW"
    Then the response status is 201
    And the ticket priority is "HIGH"