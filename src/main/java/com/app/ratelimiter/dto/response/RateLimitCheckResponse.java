package com.app.ratelimiter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of a rate limit check for an application")
public record RateLimitCheckResponse(
        @Schema(description = "Application identifier that was checked", example = "payments-service")
        String appId,

        @Schema(description = "Whether the request was allowed to proceed", example = "true")
        boolean allowed,

        @Schema(description = "Number of tokens remaining in the bucket after this check", example = "42")
        long remainingTokens,

        @Schema(description = "Human-readable reason for the decision", example = "Request allowed")
        String reason,

        @Schema(description = "The path pattern that was matched for this check", example = "/resources")
        String matchedPattern,

        @Schema(description = "UTC timestamp of the check")
        Instant timestamp
) {}
