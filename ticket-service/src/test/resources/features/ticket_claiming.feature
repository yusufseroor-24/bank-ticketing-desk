Feature: Claiming a ticket
  Only one agent can own a ticket, even when several try at the same moment.

  Scenario: Five agents race for one ticket
    Given 5 agents who can handle "CARD_DISPUTE" tickets
    And an open "CARD_DISPUTE" ticket
    When all agents try to claim it at the same moment
    Then exactly 1 agent wins the ticket
    And every other agent gets 409

  Scenario: An agent cannot claim a ticket outside their categories
    Given an agent who only handles "COMPLAINT" tickets
    And an open "CARD_DISPUTE" ticket
    When the agent tries to claim the ticket
    Then the response status is 422