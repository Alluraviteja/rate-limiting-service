package com.app.ratelimiter.mcp.tools;

import com.app.ratelimiter.mcp.dto.AppDetail;
import com.app.ratelimiter.mcp.dto.AppSummary;
import com.app.ratelimiter.service.mcp.AppRegistryMcpService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AppRegistryTools {

    private final AppRegistryMcpService appRegistryMcpService;

    @Tool(name = "list_apps",
          description = "Returns all enabled apps registered in the rate limiting service. Call this at the start of each analysis run to determine which apps to analyze.")
    public List<AppSummary> listApps() {
        return appRegistryMcpService.listApps();
    }

    @Tool(name = "get_app",
          description = "Returns a single app by ID including all its active rate limit plans. Use this to get the configured limits and per-IP mode flag for a specific app.")
    public AppDetail getApp(
            @ToolParam(description = "The app ID to fetch") long appInfoId) {
        return appRegistryMcpService.getApp(appInfoId);
    }
}
