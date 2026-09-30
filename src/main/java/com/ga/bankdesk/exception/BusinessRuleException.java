package com.ga.bankdesk.exception;

//Thrown when the request breaks the business rule

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
