package com.ga.bankdesk.dto;

import com.ga.bankdesk.enums.SimulationScenario;
import jakarta.validation.constraints.NotNull;

public record SimulateTransactionsRequest(@NotNull Long customerId,
                                          @NotNull SimulationScenario scenario) {
}
