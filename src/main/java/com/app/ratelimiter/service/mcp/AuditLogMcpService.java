package com.app.ratelimiter.service.mcp;

import com.app.ratelimiter.mcp.dto.ErrorSummary;
import com.app.ratelimiter.mcp.dto.RecentLogEntry;
import com.app.ratelimiter.mcp.dto.TokenHealthSummary;
import com.app.ratelimiter.mcp.dto.TopPathsSummary;

import java.util.List;

public interface AuditLogMcpService {

    List<RecentLogEntry> getRecentLogs(Long appInfoId, int windowMinutes, Integer limit);

    ErrorSummary getErrorSummary(Long appInfoId, int windowMinutes);

    TokenHealthSummary getTokenHealthSummary(Long appInfoId, int windowMinutes);

    TopPathsSummary getTopPathsSummary(Long appInfoId, int windowMinutes);
}
