package com.app.ratelimiter.mcp.dto;

public record BucketEntry(
        String bucketKey,
        long remainingTokens,
        int capacity,
        double depletionPct,
        boolean isDepleted,
        boolean isNearDepletion
) {}
