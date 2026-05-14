package com.app.ratelimiter.mcp.dto;

public record PlanSummary(
        Long id,
        String pathPattern,
        Integer capacity,
        Integer refillRate,
        Integer refillPeriodSeconds,
        String description,
        Boolean enabled
) {}
