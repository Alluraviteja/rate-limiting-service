package com.app.ratelimiter.mcp.dto;

public record AppSummary(
        Long id,
        String serviceName,
        String displayName,
        Integer servicePort,
        String description,
        Boolean enabled,
        Boolean perIpAddress,
        Boolean failOpen
) {}
