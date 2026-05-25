package com.app.ratelimiter.mcp.dto;

public record RedisFailureStats(
        int windowMinutes,
        int totalRequests,
        int redisFailureRequests,
        double redisFailurePct
) {}
