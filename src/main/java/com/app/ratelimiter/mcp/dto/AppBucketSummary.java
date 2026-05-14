package com.app.ratelimiter.mcp.dto;

public record AppBucketSummary(
        Long appInfoId,
        String serviceName,
        int totalBuckets,
        int depletedBuckets,
        int nearDepletionBuckets,
        double avgRemainingPct,
        long minRemainingTokens
) {}
