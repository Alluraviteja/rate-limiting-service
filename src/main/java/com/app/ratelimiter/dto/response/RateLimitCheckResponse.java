package com.app.ratelimiter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of a rate limit check for an application")
public record RateLimitCheckResponse(
        @Schema(description = "Service name that was checked", example = "payments-service")
        String serviceName,

        @Schema(description = "Full URL of the service for the API gateway to forward the request to", example = "http://payments-service:8081")
        String serviceUrl,

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
