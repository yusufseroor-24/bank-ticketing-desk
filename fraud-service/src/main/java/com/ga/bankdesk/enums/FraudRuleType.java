package com.ga.bankdesk.enums;

public enum FraudRuleType {
    LARGE_AMOUNT, //a transaction at or above the baseline limit
    STRUCTURING, //multiple cash deposits just under the reporting limit
    VELOCITY, //too many transaction in a short period of time
    RISKY_COUNTRY //transfer to a watch listed country
}
