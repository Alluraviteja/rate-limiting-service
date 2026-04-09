package com.app.ratelimiter.dto.response;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String status,
        int code,
        String message,
        Map<String, String> fieldErrors,
        Instant timestamp
) {}
