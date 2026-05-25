package com.app.ratelimiter.mcp.tools;

import com.app.ratelimiter.mcp.dto.AppDetail;
import com.app.ratelimiter.mcp.dto.AppSummary;
import com.app.ratelimiter.service.mcp.AppRegistryMcpService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AppRegistryTools {

    private final AppRegistryMcpService appRegistryMcpService;

    @McpTool(
            name = "list_apps",
            description = "Returns all enabled apps registered in the rate limiting service. Call this at the start of each analysis run to determine which apps to analyze.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false)
    )
    public List<AppSummary> listApps() {
        return appRegistryMcpService.listApps();
    }

    @McpTool(
            name = "get_app",
            description = "Returns a single app by ID including all its active rate limit plans. Use this to get the configured limits and per-IP mode flag for a specific app.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false)
    )
    public AppDetail getApp(
            @McpToolParam(description = "The app ID to fetch") long appInfoId) {
        return appRegistryMcpService.getApp(appInfoId);
    }
}
