package com.app.ratelimiter.service.mcp;

import com.app.ratelimiter.mcp.dto.AppDetail;
import com.app.ratelimiter.mcp.dto.AppSummary;

import java.util.List;

public interface AppRegistryMcpService {

    List<AppSummary> listApps();

    AppDetail getApp(Long appInfoId);
}
