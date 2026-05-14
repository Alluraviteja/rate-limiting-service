package com.app.ratelimiter.mcp.config;

import com.app.ratelimiter.mcp.tools.AppRegistryTools;
import com.app.ratelimiter.mcp.tools.AuditLogTools;
import com.app.ratelimiter.mcp.tools.BucketStateTools;
import com.app.ratelimiter.mcp.tools.ServiceHealthTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider rateLimiterToolProvider(
            AuditLogTools auditLogTools,
            AppRegistryTools appRegistryTools,
            BucketStateTools bucketStateTools,
            ServiceHealthTools serviceHealthTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(auditLogTools, appRegistryTools, bucketStateTools, serviceHealthTools)
                .build();
    }
}
