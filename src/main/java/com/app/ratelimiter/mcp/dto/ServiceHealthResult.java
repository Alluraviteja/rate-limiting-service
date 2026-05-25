package com.app.ratelimiter.mcp.dto;

import java.time.Instant;

public record ServiceHealthResult(
        String status,
        String database,
        String redis,
        Instant checkedAt
) {}
