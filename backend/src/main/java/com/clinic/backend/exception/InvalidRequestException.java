package com.clinic.backend.exception;

// The request breaks a business rule and can never succeed as sent (HTTP 400).
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
