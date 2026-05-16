package com.app.ratelimiter.mcp.dto;

import java.time.Instant;

public record RecentLogEntry(
        Long id,
        Long appInfoId,
        String clientIp,
        Boolean wasBlocked,
        String reason,
        String httpMethod,
        String requestPath,
        Long remainingTokens,
        Integer responseCode,
        Instant requestAt,
        Long retryAfterSeconds,
        Boolean redisFailed,
        Boolean isBot,
        String botName,
        String deviceType
) {}
