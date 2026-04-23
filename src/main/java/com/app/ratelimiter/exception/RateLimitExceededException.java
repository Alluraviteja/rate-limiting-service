package com.app.ratelimiter.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends AppException {

    private final long retryAfterSeconds;
    private final long capacity;

    public RateLimitExceededException(String message, long retryAfterSeconds, long capacity) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = retryAfterSeconds;
        this.capacity = capacity;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public long getCapacity() {
        return capacity;
    }
}
