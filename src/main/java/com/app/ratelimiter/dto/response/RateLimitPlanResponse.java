package com.app.ratelimiter.dto.response;

import java.time.Instant;

public record RateLimitPlanResponse(
        Long id,
        String appId,
        int capacity,
        int refillRate,
        int refillPeriodSeconds,
        String description,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {}
