package com.app.ratelimiter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit log entry for a single rate limit check event")
public record RateLimitLogResponse(
        @Schema(description = "Unique log entry ID", example = "1001")
        Long id,

        @Schema(description = "Application identifier that was checked", example = "payments-service")
        String appId,

        @Schema(description = "Client IP address of the originating request", example = "203.0.113.42")
        String clientIp,

        @Schema(description = "Whether the request was blocked", example = "false")
        boolean wasBlocked,

        @Schema(description = "Reason for the allow or block decision", example = "Request allowed")
        String reason,

        @Schema(description = "UTC timestamp when the event was recorded")
        Instant createdAt
) {}
