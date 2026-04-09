package com.app.ratelimiter.dto.response;

import java.time.Instant;

public record RateLimitCheckResponse(
        String appId,
        boolean allowed,
        long remainingTokens,
        String reason,
        Instant timestamp
) {}
