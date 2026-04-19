package com.app.ratelimiter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Audit log entry for a single rate limit check event")
public record RateLimitLogResponse(
        @Schema(description = "Unique log entry ID", example = "1001")
        Long id,

        @Schema(description = "app_info.id reference for the app that was checked", example = "1")
        Long appInfoId,

        @Schema(description = "Client IP address of the originating request", example = "203.0.113.42")
        String clientIp,

        @Schema(description = "Whether the request was blocked", example = "false")
        boolean wasBlocked,

        @Schema(description = "Reason for the allow or block decision", example = "Rate limit exceeded")
        String reason,

        @Schema(description = "HTTP method of the request", example = "POST")
        String httpMethod,

        @Schema(description = "UTC timestamp when the event was recorded")
        Instant createdAt
) {}
