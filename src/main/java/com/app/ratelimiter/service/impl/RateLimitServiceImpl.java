package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.config.AppProperties;
import com.app.ratelimiter.config.FailureStrategy;
import com.app.ratelimiter.dto.request.RateLimitCheckRequest;
import com.app.ratelimiter.dto.response.RateLimitCheckResponse;
import com.app.ratelimiter.exception.RateLimitExceededException;
import com.app.ratelimiter.exception.ServiceUnavailableException;
import com.app.ratelimiter.model.AppInfo;
import com.app.ratelimiter.repository.AppInfoRepository;
import com.app.ratelimiter.service.RateLimitLogService;
import com.app.ratelimiter.service.RateLimitPlanService;
import com.app.ratelimiter.service.RateLimitService;
import com.app.ratelimiter.service.ResolvedBucketConfig;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class RateLimitServiceImpl implements RateLimitService {

    private static final String BUCKET_KEY_PREFIX = "rate_limit:";

    private final RateLimitPlanService planService;
    private final RateLimitLogService logService;
    private final AppInfoRepository appInfoRepository;
    private final LettuceBasedProxyManager<String> proxyManager;
    private final AppProperties appProperties;

    public RateLimitServiceImpl(
            RateLimitPlanService planService,
            RateLimitLogService logService,
            AppInfoRepository appInfoRepository,
            @Lazy LettuceBasedProxyManager<String> proxyManager,
            AppProperties appProperties) {
        this.planService = planService;
        this.logService = logService;
        this.appInfoRepository = appInfoRepository;
        this.proxyManager = proxyManager;
        this.appProperties = appProperties;
    }

    @Override
    public RateLimitCheckResponse check(RateLimitCheckRequest request) {
        String serviceIdentifier = request.serviceIdentifier();
        String clientIp = request.clientIp();
        String requestPath = request.requestPath();
        String httpMethod = request.httpMethod();

        Optional<AppInfo> appOpt = findByServiceNameOrPort(serviceIdentifier);
        if (appOpt.isEmpty()) {
            log.debug("No app registered for serviceIdentifier={}, allowing request", serviceIdentifier);
            safeLog(null, clientIp, false, "No app registered for serviceIdentifier", httpMethod);
            return new RateLimitCheckResponse(serviceIdentifier, null, true, -1, "No app registered for serviceIdentifier", null, Instant.now());
        }

        AppInfo app = appOpt.get();
        Long appInfoId = app.getId();
        String serviceName = app.getServiceName();
        String serviceUrl = app.getServiceUrl();

        ResolvedBucketConfig resolved;
        try {
            resolved = planService.getBucketConfiguration(appInfoId, requestPath);
        } catch (Exception e) {
            log.error("Failed to load bucket configuration for appInfoId={}: {}", appInfoId, e.getMessage());
            throw e;
        }

        if (!resolved.hasMatch()) {
            safeLog(appInfoId, clientIp, false, "No rate limit plan configured", httpMethod);
            log.debug("No plan matched for serviceName={}, path={} — allowing request", serviceName, requestPath);
            return new RateLimitCheckResponse(serviceName, serviceUrl, true, -1, "No rate limit plan configured", null, Instant.now());
        }

        String bucketKey = BUCKET_KEY_PREFIX + appInfoId + ":" + resolved.matchedPattern();
        try {
            BucketProxy bucket = proxyManager.builder().build(bucketKey, () -> resolved.config());
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                safeLog(appInfoId, clientIp, false, null, httpMethod);
                log.debug("Request allowed for serviceName={}, path={}, remaining={}", serviceName, requestPath, probe.getRemainingTokens());
                return new RateLimitCheckResponse(serviceName, serviceUrl, true, probe.getRemainingTokens(), null, resolved.matchedPattern(), Instant.now());
            }

            long retryAfterSeconds = (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0);
            safeLog(appInfoId, clientIp, true, "Rate limit exceeded", httpMethod);
            log.warn("Rate limit exceeded for serviceName={}, path={}, clientIp={}, retryAfter={}s", serviceName, requestPath, clientIp, retryAfterSeconds);
            throw new RateLimitExceededException("Rate limit exceeded for serviceName: " + serviceName, retryAfterSeconds);

        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis error for appInfoId={}, clientIp={}: {}", appInfoId, clientIp, e.getMessage());
            return handleRedisFailure(serviceName, serviceUrl, appInfoId, clientIp, resolved.matchedPattern(), httpMethod);
        }
    }

    private RateLimitCheckResponse handleRedisFailure(String serviceName, String serviceUrl, Long appInfoId, String clientIp, String matchedPattern, String httpMethod) {
        FailureStrategy strategy = appProperties.getRatelimit().getRedis().getFailureStrategy();
        if (strategy == FailureStrategy.FAIL_CLOSED) {
            safeLog(appInfoId, clientIp, true, "Redis unavailable - fail closed", httpMethod);
            throw new ServiceUnavailableException("Rate limit service temporarily unavailable");
        }
        safeLog(appInfoId, clientIp, false, "Redis unavailable - fail open", httpMethod);
        log.warn("Failing open for serviceName={} due to Redis unavailability", serviceName);
        return new RateLimitCheckResponse(serviceName, serviceUrl, true, -1, "Redis unavailable - fail open", matchedPattern, Instant.now());
    }

    private Optional<AppInfo> findByServiceNameOrPort(String identifier) {
        Optional<AppInfo> byName = appInfoRepository.findByServiceName(identifier);
        if (byName.isPresent()) {
            return byName;
        }
        try {
            return appInfoRepository.findByServicePort(Integer.parseInt(identifier));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void safeLog(Long appInfoId, String clientIp, boolean wasBlocked, String reason, String httpMethod) {
        try {
            logService.log(appInfoId, clientIp, wasBlocked, reason, httpMethod);
        } catch (Exception e) {
            log.warn("Failed to write audit log for appInfoId={}: {}", appInfoId, e.getMessage());
        }
    }
}
