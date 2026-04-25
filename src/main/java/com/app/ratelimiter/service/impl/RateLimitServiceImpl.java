package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.config.AppProperties;
import com.app.ratelimiter.config.FailureStrategy;
import com.app.ratelimiter.dto.request.RateLimitCheckRequest;
import com.app.ratelimiter.dto.response.RateLimitCheckResponse;
import com.app.ratelimiter.exception.RateLimitExceededException;
import com.app.ratelimiter.exception.ResourceNotFoundException;
import com.app.ratelimiter.exception.ServiceUnavailableException;
import com.app.ratelimiter.model.AppInfo;
import com.app.ratelimiter.repository.AppInfoRepository;
import com.app.ratelimiter.service.RateLimitAuditEntry;
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
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimitServiceImpl implements RateLimitService {

    private static final String BUCKET_KEY_PREFIX = "rate_limit:";

    private final RateLimitPlanService planService;
    private final RateLimitLogService logService;
    private final AppInfoRepository appInfoRepository;
    private final LettuceBasedProxyManager<String> proxyManager;
    private final AppProperties appProperties;
    private final ConcurrentHashMap<String, AppInfo> appInfoCache = new ConcurrentHashMap<>();

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
        Instant requestAt = Instant.now();

        AppInfo app = findByServiceNameOrPort(request.serviceIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No app registered for serviceIdentifier: " + request.serviceIdentifier()));

        Long appInfoId   = app.getId();
        String serviceName = app.getServiceName();

        RateLimitAuditEntry.RateLimitAuditEntryBuilder auditBase = RateLimitAuditEntry.builder()
                .appInfoId(appInfoId)
                .clientIp(request.clientIp())
                .httpMethod(request.httpMethod())
                .requestPath(request.requestPath())
                .traceId(request.traceId())
                .requestAt(requestAt)
                .browser(request.browser())
                .os(request.os())
                .deviceType(request.deviceType())
                .isBot(Boolean.TRUE.equals(request.isBot()))
                .botName(request.botName())
                .requestSize(request.requestSize())
                .referer(request.referer());

        ResolvedBucketConfig resolved;
        try {
            resolved = planService.getBucketConfiguration(appInfoId, request.requestPath());
        } catch (Exception e) {
            log.error("Failed to load bucket configuration for appInfoId={}: {}", appInfoId, e.getMessage());
            throw e;
        }

        if (!resolved.hasMatch()) {
            safeLog(auditBase.wasBlocked(true).reason("No matching rate limit plan").remainingTokens(0L).responseCode(404).build());
            log.warn("No matching plan for serviceName={}, path={} — blocking request", serviceName, request.requestPath());
            throw new ResourceNotFoundException("No rate limit plan configured for path: " + request.requestPath());
        }

        String bucketKey = BUCKET_KEY_PREFIX + appInfoId + ":" + resolved.matchedPattern();
        try {
            BucketProxy bucket = proxyManager.builder().build(bucketKey, () -> resolved.config());
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                long resetAfterSeconds = (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0);
                safeLog(auditBase.wasBlocked(false).remainingTokens(probe.getRemainingTokens()).responseCode(200).build());
                log.debug("Request allowed for serviceName={}, path={}, remaining={}", serviceName, request.requestPath(), probe.getRemainingTokens());
                return new RateLimitCheckResponse(serviceName, true, probe.getRemainingTokens(), resolved.capacity(), resetAfterSeconds, null, resolved.matchedPattern(), Instant.now());
            }

            long retryAfterSeconds = (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0);
            safeLog(auditBase.wasBlocked(true).reason("Rate limit exceeded").remainingTokens(0L).responseCode(429).retryAfterSeconds(retryAfterSeconds).build());
            log.warn("Rate limit exceeded for serviceName={}, path={}, clientIp={}, retryAfter={}s", serviceName, request.requestPath(), request.clientIp(), retryAfterSeconds);
            throw new RateLimitExceededException("Rate limit exceeded for serviceName: " + serviceName, retryAfterSeconds, resolved.capacity());

        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis error for appInfoId={}, clientIp={}: {}", appInfoId, request.clientIp(), e.getMessage());
            return handleRedisFailure(serviceName, auditBase);
        }
    }

    private RateLimitCheckResponse handleRedisFailure(String serviceName, RateLimitAuditEntry.RateLimitAuditEntryBuilder auditBase) {
        FailureStrategy strategy = appProperties.getRatelimit().getRedis().getFailureStrategy();
        if (strategy == FailureStrategy.FAIL_CLOSED) {
            safeLog(auditBase.wasBlocked(true).reason("Redis unavailable - fail closed").remainingTokens(-1L).responseCode(503).redisFailed(true).build());
            throw new ServiceUnavailableException("Rate limit service temporarily unavailable");
        }
        safeLog(auditBase.wasBlocked(false).reason("Redis unavailable - fail open").remainingTokens(-1L).responseCode(200).redisFailed(true).build());
        log.warn("Failing open for serviceName={} due to Redis unavailability", serviceName);
        return new RateLimitCheckResponse(serviceName, true, -1, -1, -1, "Redis unavailable - fail open", null, Instant.now());
    }

    @Override
    public void evictAppInfoCache(String serviceName, Integer servicePort) {
        appInfoCache.remove(serviceName);
        if (servicePort != null) {
            appInfoCache.remove(String.valueOf(servicePort));
        }
    }

    private Optional<AppInfo> findByServiceNameOrPort(String identifier) {
        AppInfo cached = appInfoCache.get(identifier);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<AppInfo> byName = appInfoRepository.findByServiceName(identifier);
        if (byName.isPresent()) {
            cacheAppInfo(byName.get());
            return byName;
        }
        try {
            Optional<AppInfo> byPort = appInfoRepository.findByServicePort(Integer.parseInt(identifier));
            byPort.ifPresent(this::cacheAppInfo);
            return byPort;
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void cacheAppInfo(AppInfo appInfo) {
        appInfoCache.put(appInfo.getServiceName(), appInfo);
        if (appInfo.getServicePort() != null) {
            appInfoCache.put(String.valueOf(appInfo.getServicePort()), appInfo);
        }
    }

    private void safeLog(RateLimitAuditEntry entry) {
        try {
            logService.log(entry);
        } catch (Exception e) {
            log.warn("Failed to write audit log for appInfoId={}: {}", entry.getAppInfoId(), e.getMessage());
        }
    }
}
