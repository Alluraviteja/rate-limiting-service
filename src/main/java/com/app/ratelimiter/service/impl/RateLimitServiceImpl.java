package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.config.AppProperties;
import com.app.ratelimiter.config.FailureStrategy;
import com.app.ratelimiter.dto.request.RateLimitCheckRequest;
import com.app.ratelimiter.dto.response.RateLimitCheckResponse;
import com.app.ratelimiter.exception.RateLimitExceededException;
import com.app.ratelimiter.exception.ServiceUnavailableException;
import com.app.ratelimiter.service.RateLimitLogService;
import com.app.ratelimiter.service.RateLimitPlanService;
import com.app.ratelimiter.service.RateLimitService;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final String BUCKET_KEY_PREFIX = "rate_limit:";

    private final RateLimitPlanService planService;
    private final RateLimitLogService logService;
    private final LettuceBasedProxyManager<String> proxyManager;
    private final AppProperties appProperties;

    @Override
    public RateLimitCheckResponse check(RateLimitCheckRequest request) {
        String appId = request.appId();
        String clientIp = request.clientIp();

        BucketConfiguration config;
        try {
            config = planService.getBucketConfiguration(appId);
        } catch (Exception e) {
            log.error("Failed to load bucket configuration for appId={}: {}", appId, e.getMessage());
            throw e;
        }

        String bucketKey = BUCKET_KEY_PREFIX + appId;
        try {
            BucketProxy bucket = proxyManager.builder().build(bucketKey, () -> config);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                safeLog(appId, clientIp, false, null);
                log.debug("Request allowed for appId={}, clientIp={}, remaining={}", appId, clientIp, probe.getRemainingTokens());
                return new RateLimitCheckResponse(appId, true, probe.getRemainingTokens(), null, Instant.now());
            }

            safeLog(appId, clientIp, true, "Rate limit exceeded");
            log.warn("Rate limit exceeded for appId={}, clientIp={}", appId, clientIp);
            throw new RateLimitExceededException("Rate limit exceeded for appId: " + appId);

        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis error for appId={}, clientIp={}: {}", appId, clientIp, e.getMessage());
            return handleRedisFailure(appId, clientIp);
        }
    }

    private RateLimitCheckResponse handleRedisFailure(String appId, String clientIp) {
        FailureStrategy strategy = appProperties.getRatelimit().getRedis().getFailureStrategy();
        if (strategy == FailureStrategy.FAIL_CLOSED) {
            safeLog(appId, clientIp, true, "Redis unavailable - fail closed");
            throw new ServiceUnavailableException("Rate limit service temporarily unavailable");
        }
        safeLog(appId, clientIp, false, "Redis unavailable - fail open");
        log.warn("Failing open for appId={} due to Redis unavailability", appId);
        return new RateLimitCheckResponse(appId, true, -1, "Redis unavailable - fail open", Instant.now());
    }

    private void safeLog(String appId, String clientIp, boolean wasBlocked, String reason) {
        try {
            logService.log(appId, clientIp, wasBlocked, reason);
        } catch (Exception e) {
            log.warn("Failed to write audit log for appId={}: {}", appId, e.getMessage());
        }
    }
}
