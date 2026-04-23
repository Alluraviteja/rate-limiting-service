package com.app.ratelimiter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of a rate limit check for an application")
public record RateLimitCheckResponse(
        @Schema(description = "Service name that was checked", example = "payments-service")
        String serviceName,

        @Schema(description = "Whether the request was allowed to proceed", example = "true")
        boolean allowed,

        @Schema(description = "Number of tokens remaining in the bucket after this check (-1 when no plan applies)", example = "42")
        long remainingTokens,

        @Schema(description = "Bucket capacity (max tokens). Mirrors the RateLimit-Limit response header. -1 when no plan applies.", example = "100")
        long limit,

        @Schema(description = "Seconds until the bucket refills to full capacity. Mirrors the RateLimit-Reset response header. -1 when no plan applies.", example = "12")
        long resetAfterSeconds,

        @Schema(description = "Human-readable reason for the decision", example = "Request allowed")
        String reason,

        @Schema(description = "The path pattern that was matched for this check", example = "/resources")
        String matchedPattern,

        @Schema(description = "UTC timestamp of the check")
        Instant timestamp
) {}
