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

        @Schema(description = "Request path that was checked against rate limit plans", example = "/api/orders")
        String requestPath,

        @Schema(description = "Remaining tokens in the bucket after this request (-1 = not applicable)", example = "42")
        Long remainingTokens,

        @Schema(description = "Trace/correlation ID from the upstream request header", example = "abc123def456")
        String traceId,

        @Schema(description = "HTTP response code sent back to the caller (200, 429, 503)", example = "429")
        Integer responseCode,

        @Schema(description = "Precise timestamp when the request entered the rate limiter (before Redis evaluation)")
        Instant requestAt,

        @Schema(description = "Seconds until the next request will be allowed — only present on 429 responses", example = "14")
        Long retryAfterSeconds,

        @Schema(description = "Whether this event was caused by a Redis failure", example = "false")
        boolean redisFailed,

        @Schema(description = "UTC timestamp when the audit record was persisted")
        Instant createdAt,

        @Schema(description = "Browser name parsed from User-Agent", example = "Chrome")
        String browser,

        @Schema(description = "Browser major.minor version", example = "120.0")
        String browserVersion,

        @Schema(description = "Operating system name", example = "Windows")
        String os,

        @Schema(description = "Operating system version", example = "10/11")
        String osVersion,

        @Schema(description = "Device category: mobile, tablet, desktop, or bot", example = "desktop")
        String deviceType,

        @Schema(description = "Whether the request came from a bot", example = "false")
        boolean isBot,

        @Schema(description = "Bot name if identified (e.g. Googlebot), null otherwise", example = "Googlebot")
        String botName,

        @Schema(description = "Raw User-Agent string", example = "Mozilla/5.0 ...")
        String userAgent,

        @Schema(description = "Size of the original request body in bytes", example = "512")
        Long requestSize,

        @Schema(description = "Referer header from the original request", example = "https://google.com")
        String referer
) {}
