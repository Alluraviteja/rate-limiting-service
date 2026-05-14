package com.app.ratelimiter.mcp.dto;

import java.util.List;

public record AppDetail(
        Long id,
        String serviceName,
        Integer servicePort,
        String description,
        Boolean enabled,
        Boolean perIpAddress,
        Boolean failOpen,
        List<PlanSummary> plans
) {}
