package com.app.ratelimiter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Rate limit plan configuration for an application")
public record RateLimitPlanResponse(
        @Schema(description = "Unique plan ID", example = "1")
        Long id,

        @Schema(description = "app_info.id reference for the app this plan applies to", example = "1")
        Long appInfoId,

        @Schema(description = "Ant-style path pattern this plan applies to", example = "/resources")
        String pathPattern,

        @Schema(description = "Maximum bucket token capacity", example = "100")
        int capacity,

        @Schema(description = "Tokens added per refill period", example = "10")
        int refillRate,

        @Schema(description = "Refill period duration in seconds", example = "60")
        int refillPeriodSeconds,

        @Schema(description = "Human-readable description", example = "Standard plan for payments-service")
        String description,

        @Schema(description = "Whether this plan is currently active", example = "true")
        boolean enabled,

        @Schema(description = "UTC timestamp when the plan was created")
        Instant createdAt,

        @Schema(description = "UTC timestamp when the plan was last updated")
        Instant updatedAt
) {}
