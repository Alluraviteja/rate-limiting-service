package com.app.ratelimiter.service.mcp.impl;

import com.app.ratelimiter.dto.response.AppInfoResponse;
import com.app.ratelimiter.dto.response.RateLimitPlanResponse;
import com.app.ratelimiter.mcp.dto.AppBucketSummary;
import com.app.ratelimiter.mcp.dto.BucketEntry;
import com.app.ratelimiter.mcp.dto.BucketStateResult;
import com.app.ratelimiter.service.AppInfoService;
import com.app.ratelimiter.service.RateLimitPlanService;
import com.app.ratelimiter.service.mcp.BucketStateMcpService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BucketStateMcpServiceImpl implements BucketStateMcpService {

    private static final String BUCKET_KEY_PREFIX = "rate_limit:";
    private static final int MAX_BUCKETS_PER_APP = 100;
    private static final int SCAN_COUNT_HINT = 200;
    private static final double NEAR_DEPLETION_THRESHOLD = 0.10;

    private final AppInfoService appInfoService;
    private final RateLimitPlanService planService;
    private final LettuceBasedProxyManager<String> proxyManager;
    private final StatefulRedisConnection<String, byte[]> bucketRedisConnection;

    public BucketStateMcpServiceImpl(
            AppInfoService appInfoService,
            RateLimitPlanService planService,
            @Lazy LettuceBasedProxyManager<String> proxyManager,
            StatefulRedisConnection<String, byte[]> bucketRedisConnection) {
        this.appInfoService = appInfoService;
        this.planService = planService;
        this.proxyManager = proxyManager;
        this.bucketRedisConnection = bucketRedisConnection;
    }

    @Override
    public BucketStateResult getBucketState(Long appInfoId) {
        AppInfoResponse app = appInfoService.getById(appInfoId);
        List<RateLimitPlanResponse> plans = planService.getEnabledByAppInfoId(appInfoId);
        List<BucketEntry> buckets = resolveActiveBuckets(appInfoId, plans);
        return new BucketStateResult(appInfoId, app.serviceName(), app.perIpAddress(), buckets);
    }

    @Override
    public List<AppBucketSummary> getAllBucketStates() {
        return appInfoService.getAll(Pageable.unpaged()).getContent().stream()
                .filter(AppInfoResponse::enabled)
                .map(app -> {
                    List<RateLimitPlanResponse> plans = planService.getEnabledByAppInfoId(app.id());
                    List<BucketEntry> buckets = resolveActiveBuckets(app.id(), plans);
                    return toAppBucketSummary(app, buckets);
                })
                .toList();
    }

    private List<BucketEntry> resolveActiveBuckets(Long appInfoId, List<RateLimitPlanResponse> plans) {
        if (plans.isEmpty()) {
            return List.of();
        }
        String pattern = BUCKET_KEY_PREFIX + appInfoId + ":*";
        List<String> keys = scanKeys(pattern, MAX_BUCKETS_PER_APP);

        List<BucketEntry> entries = new ArrayList<>();
        for (String key : keys) {
            RateLimitPlanResponse plan = matchPlanForKey(key, appInfoId, plans);
            if (plan == null) {
                continue;
            }
            BucketEntry entry = readBucketEntry(key, plan);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private List<String> scanKeys(String pattern, int maxKeys) {
        RedisCommands<String, byte[]> commands = bucketRedisConnection.sync();
        ScanArgs args = ScanArgs.Builder.matches(pattern).limit(SCAN_COUNT_HINT);
        List<String> keys = new ArrayList<>();
        try {
            io.lettuce.core.KeyScanCursor<String> cursor = commands.scan(args);
            keys.addAll(cursor.getKeys());
            while (!cursor.isFinished() && keys.size() < maxKeys) {
                cursor = commands.scan(ScanCursor.of(cursor.getCursor()), args);
                keys.addAll(cursor.getKeys());
            }
        } catch (Exception e) {
            log.error("Redis SCAN failed for pattern={}: {}", pattern, e.getMessage());
            throw e;
        }
        return keys.size() > maxKeys ? keys.subList(0, maxKeys) : keys;
    }

    private RateLimitPlanResponse matchPlanForKey(String key, Long appInfoId, List<RateLimitPlanResponse> plans) {
        String prefix = BUCKET_KEY_PREFIX + appInfoId + ":";
        if (!key.startsWith(prefix)) {
            return null;
        }
        String rest = key.substring(prefix.length());
        return plans.stream()
                .filter(p -> rest.equals(p.pathPattern()) || rest.startsWith(p.pathPattern() + ":"))
                .findFirst()
                .orElse(null);
    }

    private BucketEntry readBucketEntry(String key, RateLimitPlanResponse plan) {
        try {
            BucketConfiguration config = buildConfig(plan);
            BucketProxy bucket = proxyManager.builder().build(key, () -> config);
            long remaining = bucket.getAvailableTokens();
            int capacity = plan.capacity();
            double depletionPct = capacity > 0 ? Math.round(((capacity - remaining) * 100.0 / capacity) * 100.0) / 100.0 : 0;
            boolean depleted = remaining == 0;
            boolean nearDepletion = remaining <= Math.round(capacity * NEAR_DEPLETION_THRESHOLD);
            return new BucketEntry(key, remaining, capacity, depletionPct, depleted, nearDepletion);
        } catch (Exception e) {
            log.warn("Failed to read bucket state for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private BucketConfiguration buildConfig(RateLimitPlanResponse plan) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(plan.capacity())
                        .refillGreedy(plan.refillRate(), Duration.ofSeconds(plan.refillPeriodSeconds()))
                        .build())
                .build();
    }

    private AppBucketSummary toAppBucketSummary(AppInfoResponse app, List<BucketEntry> buckets) {
        int total = buckets.size();
        int depleted = (int) buckets.stream().filter(BucketEntry::isDepleted).count();
        int nearDepletion = (int) buckets.stream().filter(BucketEntry::isNearDepletion).count();
        double avgPct = total == 0 ? 100.0 : buckets.stream()
                .mapToDouble(b -> b.capacity() > 0 ? (b.remainingTokens() * 100.0 / b.capacity()) : 100.0)
                .average()
                .orElse(100.0);
        long minRemaining = total == 0 ? 0 : buckets.stream().mapToLong(BucketEntry::remainingTokens).min().orElse(0);
        return new AppBucketSummary(
                app.id(), app.serviceName(), total, depleted, nearDepletion,
                Math.round(avgPct * 100.0) / 100.0, minRemaining
        );
    }
}
