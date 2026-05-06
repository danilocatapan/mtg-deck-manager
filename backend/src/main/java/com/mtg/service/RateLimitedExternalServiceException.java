package com.mtg.service;

public class RateLimitedExternalServiceException extends ExternalServiceException {

    public RateLimitedExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
