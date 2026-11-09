package com.ga.bankdesk.exception;

import org.springframework.http.HttpStatus;

/**
 * third party service exception
 */

public class ExternalServiceException extends RuntimeException{

    private final HttpStatus status;

    public ExternalServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
