package com.app.ratelimiter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Configuration for a per-app token bucket rate limit plan")
public record RateLimitPlanRequest(
        @Schema(description = "Unique application identifier this plan applies to", example = "payments-service")
        @NotBlank @Size(max = 255) String appId,

        @Schema(description = "Maximum number of tokens the bucket can hold", example = "100")
        @Positive @Max(1_000_000) int capacity,

        @Schema(description = "Number of tokens added per refill period", example = "10")
        @Positive int refillRate,

        @Schema(description = "Duration of the refill period in seconds", example = "60")
        @Positive int refillPeriodSeconds,

        @Schema(description = "Optional human-readable description of the plan", example = "Standard plan for payments-service")
        @Size(max = 1000) String description
) {}
