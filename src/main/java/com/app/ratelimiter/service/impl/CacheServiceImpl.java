package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.dto.response.CacheRefreshResponse;
import com.app.ratelimiter.service.CacheService;
import com.app.ratelimiter.service.RateLimitPlanService;
import com.app.ratelimiter.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final RateLimitService rateLimitService;
    private final RateLimitPlanService rateLimitPlanService;

    @Override
    public CacheRefreshResponse refreshAll() {
        int appInfoCleared = rateLimitService.evictAllAppInfoCache();
        int planCleared = rateLimitPlanService.evictAllPlanCaches();
        log.info("Full cache refresh completed: appInfo={}, plans={}", appInfoCleared, planCleared);
        return new CacheRefreshResponse(appInfoCleared, planCleared, Instant.now());
    }
}
