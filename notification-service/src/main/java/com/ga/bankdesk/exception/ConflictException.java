package com.ga.bankdesk.exception;

//thrown when conflict happens with current data, like duplicated email

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
