package com.app.ratelimiter.service.mcp;

import com.app.ratelimiter.mcp.dto.RedisFailureStats;
import com.app.ratelimiter.mcp.dto.ServiceHealthResult;

public interface ServiceHealthMcpService {

    ServiceHealthResult getServiceHealth();

    RedisFailureStats getRedisFailureStats(Long appInfoId, int windowMinutes);
}
