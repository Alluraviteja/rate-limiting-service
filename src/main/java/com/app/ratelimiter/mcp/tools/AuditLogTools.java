package com.app.ratelimiter.mcp.tools;

import com.app.ratelimiter.mcp.dto.ErrorSummary;
import com.app.ratelimiter.mcp.dto.RecentLogEntry;
import com.app.ratelimiter.mcp.dto.TokenHealthSummary;
import com.app.ratelimiter.mcp.dto.TopPathsSummary;
import com.app.ratelimiter.service.mcp.AuditLogMcpService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuditLogTools {

    private final AuditLogMcpService auditLogMcpService;

    @McpTool(
            name = "get_recent_logs",
            description = "Fetch raw rate limit log entries for a specific app within a time window. Returns individual request records with all audit fields.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false, openWorldHint = false)
    )
    public List<RecentLogEntry> getRecentLogs(
            @McpToolParam(description = "The app to query logs for") long appInfoId,
            @McpToolParam(description = "How far back to look in minutes, e.g. 15 or 60") int windowMinutes,
            @McpToolParam(description = "Max records to return, default 5000", required = false) @Nullable Integer limit) {
        return auditLogMcpService.getRecentLogs(appInfoId, windowMinutes, limit);
    }

    @McpTool(
            name = "get_error_summary",
            description = "Returns pre-aggregated error and block metrics for an app within a time window. Includes block rate, response code breakdown, top error paths, top block reasons, and IP concentration.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false, openWorldHint = false)
    )
    public ErrorSummary getErrorSummary(
            @McpToolParam(description = "The app to summarize") long appInfoId,
            @McpToolParam(description = "Time window in minutes, typically 15") int windowMinutes) {
        return auditLogMcpService.getErrorSummary(appInfoId, windowMinutes);
    }

    @McpTool(
            name = "get_token_health_summary",
            description = "Returns pre-aggregated token bucket health metrics for an app. Shows average remaining tokens, depletion counts, near-depletion events, and per-path token consumption.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false, openWorldHint = false)
    )
    public TokenHealthSummary getTokenHealthSummary(
            @McpToolParam(description = "The app to summarize") long appInfoId,
            @McpToolParam(description = "Time window in minutes, typically 15") int windowMinutes) {
        return auditLogMcpService.getTokenHealthSummary(appInfoId, windowMinutes);
    }

    @McpTool(
            name = "get_top_paths_summary",
            description = "Returns pre-aggregated per-path traffic and block rate metrics. Shows top paths by traffic volume and by block rate. Use a 60-minute window for meaningful path analysis.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false, openWorldHint = false)
    )
    public TopPathsSummary getTopPathsSummary(
            @McpToolParam(description = "The app to summarize") long appInfoId,
            @McpToolParam(description = "Time window in minutes, typically 60") int windowMinutes) {
        return auditLogMcpService.getTopPathsSummary(appInfoId, windowMinutes);
    }
}
