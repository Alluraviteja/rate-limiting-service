package com.app.ratelimiter.dto.response;

import java.time.Instant;

public record PingResponse(
        String status,
        ComponentStatus database,
        ComponentStatus redis,
        Instant timestamp
) {
    public record ComponentStatus(boolean connected, String message) {}
}
