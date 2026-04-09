package com.app.ratelimiter.exception;

import org.springframework.http.HttpStatus;

public class InvalidPlanConfigException extends AppException {

    public InvalidPlanConfigException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
