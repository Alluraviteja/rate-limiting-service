package com.app.ratelimiter.mcp.dto;

import java.util.List;

public record ErrorSummary(
        int windowMinutes,
        int totalRequests,
        int blockedRequests,
        double blockRatePct,
        int response2xx,
        int response4xx,
        int response429,
        int response5xx,
        double errorRatePct,
        List<PathEntry> topErrorPaths,
        List<BlockReasonEntry> topBlockReasons,
        int uniqueIps,
        List<IpBlockEntry> topBlockingIps,
        double ipConcentrationPct,
        int redisFailureCount
) {}
