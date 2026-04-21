package com.app.ratelimiter.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends AppException {

    public ServiceUnavailableException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
