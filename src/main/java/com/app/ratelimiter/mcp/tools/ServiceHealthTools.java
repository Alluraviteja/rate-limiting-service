package com.app.ratelimiter.mcp.tools;

import com.app.ratelimiter.mcp.dto.RedisFailureStats;
import com.app.ratelimiter.mcp.dto.ServiceHealthResult;
import com.app.ratelimiter.service.mcp.ServiceHealthMcpService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceHealthTools {

    private final ServiceHealthMcpService serviceHealthMcpService;

    @Tool(name = "get_service_health",
          description = "Returns the current operational health of the rate-limiting-service, its database connection, and Redis connection. Check this before interpreting analysis results — Redis failures in fail-open mode distort block rates.")
    public ServiceHealthResult getServiceHealth() {
        return serviceHealthMcpService.getServiceHealth();
    }

    @Tool(name = "get_redis_failure_stats",
          description = "Returns how many requests in the last N minutes were served under Redis failure. Use this to flag whether observed block rates are meaningful or distorted by Redis downtime.")
    public RedisFailureStats getRedisFailureStats(
            @ToolParam(description = "Specific app ID to check, or omit for all apps", required = false) @Nullable Long appInfoId,
            @ToolParam(description = "Time window in minutes to check") int windowMinutes) {
        return serviceHealthMcpService.getRedisFailureStats(appInfoId, windowMinutes);
    }
}
