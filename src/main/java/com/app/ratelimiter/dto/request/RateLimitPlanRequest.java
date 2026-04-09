package com.app.ratelimiter.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RateLimitPlanRequest(
        @NotBlank @Size(max = 255) String appId,
        @Positive @Max(1_000_000) int capacity,
        @Positive int refillRate,
        @Positive int refillPeriodSeconds,
        @Size(max = 1000) String description
) {}
