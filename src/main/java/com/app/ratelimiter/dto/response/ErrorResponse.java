package com.app.ratelimiter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        int code,
        String message,
        Map<String, String> fieldErrors,
        Long retryAfterSeconds,
        Instant timestamp
) {}
