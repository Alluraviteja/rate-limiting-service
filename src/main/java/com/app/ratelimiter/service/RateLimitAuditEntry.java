package com.app.ratelimiter.service;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class RateLimitAuditEntry {
    Long appInfoId;
    String clientIp;
    boolean wasBlocked;
    String reason;
    String httpMethod;
    String requestPath;
    Long remainingTokens;
    String traceId;
    int responseCode;
    Instant requestAt;
    Long retryAfterSeconds;
    boolean redisFailed;
    String browser;
    String os;
    String deviceType;
    boolean isBot;
    String botName;
    Long requestSize;
    String referer;
}
