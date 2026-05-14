package com.app.ratelimiter.service.mcp.impl;

import com.app.ratelimiter.dto.response.AppInfoResponse;
import com.app.ratelimiter.dto.response.RateLimitPlanResponse;
import com.app.ratelimiter.mcp.dto.AppDetail;
import com.app.ratelimiter.mcp.dto.AppSummary;
import com.app.ratelimiter.mcp.dto.PlanSummary;
import com.app.ratelimiter.service.AppInfoService;
import com.app.ratelimiter.service.RateLimitPlanService;
import com.app.ratelimiter.service.mcp.AppRegistryMcpService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppRegistryMcpServiceImpl implements AppRegistryMcpService {

    private final AppInfoService appInfoService;
    private final RateLimitPlanService planService;

    @Override
    public List<AppSummary> listApps() {
        return appInfoService.getAll(Pageable.unpaged()).getContent().stream()
                .filter(AppInfoResponse::enabled)
                .map(this::toAppSummary)
                .toList();
    }

    @Override
    public AppDetail getApp(Long appInfoId) {
        AppInfoResponse app = appInfoService.getById(appInfoId);
        List<PlanSummary> plans = planService.getEnabledByAppInfoId(appInfoId).stream()
                .map(this::toPlanSummary)
                .toList();
        return toAppDetail(app, plans);
    }

    private AppSummary toAppSummary(AppInfoResponse app) {
        return new AppSummary(
                app.id(), app.serviceName(), app.servicePort(),
                app.description(), app.enabled(), app.perIpAddress(), app.failOpen()
        );
    }

    private AppDetail toAppDetail(AppInfoResponse app, List<PlanSummary> plans) {
        return new AppDetail(
                app.id(), app.serviceName(), app.servicePort(),
                app.description(), app.enabled(), app.perIpAddress(), app.failOpen(),
                plans
        );
    }

    private PlanSummary toPlanSummary(RateLimitPlanResponse plan) {
        return new PlanSummary(
                plan.id(), plan.pathPattern(), plan.capacity(),
                plan.refillRate(), plan.refillPeriodSeconds(),
                plan.description(), plan.enabled()
        );
    }
}
