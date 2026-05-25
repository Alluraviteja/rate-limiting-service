package com.app.ratelimiter.mcp.tools;

import com.app.ratelimiter.mcp.dto.RedisFailureStats;
import com.app.ratelimiter.mcp.dto.ServiceHealthResult;
import com.app.ratelimiter.service.mcp.ServiceHealthMcpService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceHealthTools {

    private final ServiceHealthMcpService serviceHealthMcpService;

    @McpTool(
            name = "get_service_health",
            description = "Returns the current operational health of the rate-limiting-service, its database connection, and Redis connection. Check this before interpreting analysis results — Redis failures in fail-open mode distort block rates.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false, openWorldHint = true)
    )
    public ServiceHealthResult getServiceHealth() {
        return serviceHealthMcpService.getServiceHealth();
    }

    @McpTool(
            name = "get_redis_failure_stats",
            description = "Returns how many requests in the last N minutes were served under Redis failure. Use this to flag whether observed block rates are meaningful or distorted by Redis downtime.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false, openWorldHint = false)
    )
    public RedisFailureStats getRedisFailureStats(
            @McpToolParam(description = "Specific app ID to check, or omit for all apps", required = false) @Nullable Long appInfoId,
            @McpToolParam(description = "Time window in minutes to check") int windowMinutes) {
        return serviceHealthMcpService.getRedisFailureStats(appInfoId, windowMinutes);
    }
}
