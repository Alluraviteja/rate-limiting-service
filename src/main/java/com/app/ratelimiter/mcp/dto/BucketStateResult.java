package com.app.ratelimiter.mcp.dto;

import java.util.List;

public record BucketStateResult(
        Long appInfoId,
        String serviceName,
        Boolean perIpAddress,
        List<BucketEntry> buckets
) {}
