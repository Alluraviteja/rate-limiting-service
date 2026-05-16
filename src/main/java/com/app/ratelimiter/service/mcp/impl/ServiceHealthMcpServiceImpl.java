package com.app.ratelimiter.service.mcp.impl;

import com.app.ratelimiter.config.AppProperties;
import com.app.ratelimiter.config.FailureStrategy;
import com.app.ratelimiter.mcp.dto.RedisFailureStats;
import com.app.ratelimiter.mcp.dto.ServiceHealthResult;
import com.app.ratelimiter.model.AppInfo;
import com.app.ratelimiter.model.RateLimitLog;
import com.app.ratelimiter.repository.AppInfoRepository;
import com.app.ratelimiter.repository.RateLimitLogRepository;
import com.app.ratelimiter.service.mcp.ServiceHealthMcpService;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceHealthMcpServiceImpl implements ServiceHealthMcpService {

    private static final int HEALTH_LOG_LIMIT = 10_000;
    private static final String STATUS_HEALTHY = "healthy";
    private static final String STATUS_DEGRADED = "degraded";
    private static final String STATUS_UNHEALTHY = "unhealthy";
    private static final String FAIL_OPEN_INTERPRETATION =
            "Block rates may be understated — Redis was unavailable during this window";
    private static final String NORMAL_INTERPRETATION = "Redis operating normally in this window";

    private final AppInfoRepository appInfoRepository;
    private final RateLimitLogRepository logRepository;
    private final StatefulRedisConnection<String, byte[]> bucketRedisConnection;
    private final AppProperties appProperties;

    @Override
    public ServiceHealthResult getServiceHealth() {
        Instant checkedAt = Instant.now();
        String dbStatus = checkDatabase();
        String redisStatus = checkRedis();
        String overall = determineOverallStatus(dbStatus, redisStatus);
        String failureStrategy = appProperties.getRatelimit().getRedis().getFailureStrategy() == FailureStrategy.FAIL_OPEN
                ? "fail-open" : "fail-closed";
        return new ServiceHealthResult(overall, dbStatus, redisStatus, failureStrategy, checkedAt);
    }

    @Override
    @Transactional(readOnly = true)
    public RedisFailureStats getRedisFailureStats(Long appInfoId, int windowMinutes) {
        Instant cutoff = Instant.now().minusSeconds((long) windowMinutes * 60);
        List<RateLimitLog> logs = loadLogs(appInfoId, cutoff);

        int total = logs.size();
        int redisFailures = (int) logs.stream().filter(l -> Boolean.TRUE.equals(l.getRedisFailed())).count();
        double failurePct = total == 0 ? 0.0 : Math.round((redisFailures * 100.0 / total) * 100.0) / 100.0;

        String failureStrategy = resolveFailureStrategy(appInfoId);
        String interpretation = buildInterpretation(failureStrategy, redisFailures);

        return new RedisFailureStats(windowMinutes, total, redisFailures, failurePct, failureStrategy, interpretation);
    }

    private String checkDatabase() {
        try {
            appInfoRepository.count();
            return "ok";
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return "error: " + e.getMessage();
        }
    }

    private String checkRedis() {
        try {
            bucketRedisConnection.sync().ping();
            return "ok";
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return "error: " + e.getMessage();
        }
    }

    private String determineOverallStatus(String dbStatus, String redisStatus) {
        boolean dbOk = "ok".equals(dbStatus);
        boolean redisOk = "ok".equals(redisStatus);
        if (dbOk && redisOk) {
            return STATUS_HEALTHY;
        }
        if (!dbOk && !redisOk) {
            return STATUS_UNHEALTHY;
        }
        return STATUS_DEGRADED;
    }

    private List<RateLimitLog> loadLogs(Long appInfoId, Instant cutoff) {
        if (appInfoId != null) {
            return logRepository.findRecentLogs(appInfoId, cutoff, HEALTH_LOG_LIMIT);
        }
        return logRepository.findRecentLogsAllApps(cutoff, HEALTH_LOG_LIMIT);
    }

    private String resolveFailureStrategy(Long appInfoId) {
        if (appInfoId != null) {
            return appInfoRepository.findById(appInfoId)
                    .map(AppInfo::getFailOpen)
                    .map(fo -> Boolean.TRUE.equals(fo) ? "fail-open" : "fail-closed")
                    .orElse("fail-open");
        }
        return appProperties.getRatelimit().getRedis().getFailureStrategy() == FailureStrategy.FAIL_OPEN
                ? "fail-open" : "fail-closed";
    }

    private String buildInterpretation(String failureStrategy, int redisFailures) {
        if ("fail-open".equals(failureStrategy) && redisFailures > 0) {
            return FAIL_OPEN_INTERPRETATION;
        }
        return NORMAL_INTERPRETATION;
    }
}
