package com.bikestore.api.exception;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

    private final String errorCode;
    private final Integer retryAfterSeconds;

    public ConflictException(String message) {
        super(message);
        this.errorCode = null;
        this.retryAfterSeconds = null;
    }

    public ConflictException(String message, String errorCode, Integer retryAfterSeconds) {
        super(message);
        this.errorCode = errorCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
