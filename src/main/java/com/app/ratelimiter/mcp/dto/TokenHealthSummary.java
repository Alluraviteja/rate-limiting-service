package com.app.ratelimiter.mcp.dto;

import java.util.List;

public record TokenHealthSummary(
        int windowMinutes,
        int totalRequests,
        double avgRemainingTokens,
        long minRemainingTokens,
        long maxRemainingTokens,
        int depletedCount,
        int nearDepletionCount,
        double nearDepletionPct,
        List<PathAvgEntry> topTokenConsumingPaths,
        int uniqueIps,
        int ipsNearDepletion,
        int ipsDepleted
) {}
