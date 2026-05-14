package com.app.ratelimiter.mcp.dto;

import java.util.List;

public record PathStat(
        String path,
        int total,
        int blocked,
        double blockRatePct,
        String topMethod,
        List<IpBlockEntry> topBlockingIps
) {}
