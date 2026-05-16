package com.app.ratelimiter.mcp.dto;

import java.util.List;

public record TopPathsSummary(
        int windowMinutes,
        int totalRequests,
        int uniquePaths,
        List<PathStat> topPathsByTraffic,
        List<PathStat> topPathsByBlockRate
) {}
