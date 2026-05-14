package com.app.ratelimiter.mcp.dto;

public record AppSummary(
        Long id,
        String serviceName,
        Integer servicePort,
        String description,
        Boolean enabled,
        Boolean perIpAddress,
        Boolean failOpen
) {}
