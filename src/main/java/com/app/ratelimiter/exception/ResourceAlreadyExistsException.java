package com.app.ratelimiter.exception;

import org.springframework.http.HttpStatus;

public class ResourceAlreadyExistsException extends AppException {

    public ResourceAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
