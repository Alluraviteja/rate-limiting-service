package com.app.ratelimiter.dto.response;

import java.time.Instant;

public record RateLimitLogResponse(
        Long id,
        String appId,
        String clientIp,
        boolean wasBlocked,
        String reason,
        Instant createdAt
) {}
