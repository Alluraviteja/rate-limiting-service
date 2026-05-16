package com.app.ratelimiter.service.mcp.impl;

import com.app.ratelimiter.mcp.dto.BlockReasonEntry;
import com.app.ratelimiter.mcp.dto.ErrorSummary;
import com.app.ratelimiter.mcp.dto.IpBlockEntry;
import com.app.ratelimiter.mcp.dto.PathAvgEntry;
import com.app.ratelimiter.mcp.dto.PathEntry;
import com.app.ratelimiter.mcp.dto.PathStat;
import com.app.ratelimiter.mcp.dto.RecentLogEntry;
import com.app.ratelimiter.mcp.dto.TokenHealthSummary;
import com.app.ratelimiter.mcp.dto.TopPathsSummary;
import com.app.ratelimiter.model.RateLimitLog;
import com.app.ratelimiter.repository.RateLimitLogRepository;
import com.app.ratelimiter.service.mcp.AuditLogMcpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogMcpServiceImpl implements AuditLogMcpService {

    private static final int DEFAULT_LOG_LIMIT = 5_000;
    private static final int AGGREGATE_LOG_LIMIT = 10_000;
    private static final int TOP_N = 5;
    private static final int TOP_PATHS_TRAFFIC = 10;
    private static final int TOP_PATHS_BLOCK_RATE = 5;
    private static final int MIN_REQUESTS_FOR_BLOCK_RATE = 3;
    private static final int TOP_BLOCKING_IPS_PER_PATH = 3;
    private static final double NEAR_DEPLETION_THRESHOLD = 0.10;

    private final RateLimitLogRepository logRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RecentLogEntry> getRecentLogs(Long appInfoId, int windowMinutes, Integer limit) {
        int effectiveLimit = limit != null ? limit : DEFAULT_LOG_LIMIT;
        Instant cutoff = Instant.now().minusSeconds((long) windowMinutes * 60);
        return logRepository.findRecentLogs(appInfoId, cutoff, effectiveLimit)
                .stream()
                .map(this::toRecentLogEntry)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ErrorSummary getErrorSummary(Long appInfoId, int windowMinutes) {
        List<RateLimitLog> logs = loadLogs(appInfoId, windowMinutes);
        int total = logs.size();
        if (total == 0) {
            return emptyErrorSummary(windowMinutes);
        }

        int blocked = (int) logs.stream().filter(l -> Boolean.TRUE.equals(l.getWasBlocked())).count();
        int r2xx = countByCodeRange(logs, 200, 299);
        int r4xx = countByCodeRange(logs, 400, 499);
        int r429 = (int) logs.stream().filter(l -> l.getResponseCode() != null && l.getResponseCode() == 429).count();
        int r5xx = countByCodeRange(logs, 500, 599);
        int redisFailures = (int) logs.stream().filter(l -> Boolean.TRUE.equals(l.getRedisFailed())).count();

        List<IpBlockEntry> topBlockingIps = topBlockingIps(logs, TOP_N);
        double ipConcentration = computeIpConcentration(topBlockingIps, blocked);

        return new ErrorSummary(
                windowMinutes, total, blocked,
                pct(blocked, total), r2xx, r4xx, r429, r5xx,
                pct(r4xx + r5xx, total),
                topErrorPaths(logs, TOP_N),
                topBlockReasons(logs, TOP_N),
                countUniqueIps(logs),
                topBlockingIps,
                ipConcentration,
                redisFailures
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TokenHealthSummary getTokenHealthSummary(Long appInfoId, int windowMinutes) {
        List<RateLimitLog> logs = loadLogs(appInfoId, windowMinutes);
        List<RateLimitLog> withTokens = logs.stream()
                .filter(l -> l.getRemainingTokens() != null)
                .toList();

        int total = logs.size();
        if (withTokens.isEmpty()) {
            return emptyTokenHealthSummary(windowMinutes, total);
        }

        long maxTokens = withTokens.stream().mapToLong(RateLimitLog::getRemainingTokens).max().orElse(0);
        long minTokens = withTokens.stream().mapToLong(RateLimitLog::getRemainingTokens).min().orElse(0);
        double avgTokens = withTokens.stream().mapToLong(RateLimitLog::getRemainingTokens).average().orElse(0);
        long nearDepletionThreshold = Math.round(maxTokens * NEAR_DEPLETION_THRESHOLD);

        int depleted = (int) withTokens.stream().filter(l -> l.getRemainingTokens() == 0).count();
        int nearDepletion = (int) withTokens.stream().filter(l -> l.getRemainingTokens() <= nearDepletionThreshold).count();

        return new TokenHealthSummary(
                windowMinutes, total, avgTokens, minTokens, maxTokens,
                depleted, nearDepletion, pct(nearDepletion, total),
                topTokenConsumingPaths(withTokens, TOP_N),
                countUniqueIps(logs),
                countIpsNearDepletion(withTokens, nearDepletionThreshold),
                countIpsDepleted(withTokens)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TopPathsSummary getTopPathsSummary(Long appInfoId, int windowMinutes) {
        List<RateLimitLog> logs = loadLogs(appInfoId, windowMinutes);
        int total = logs.size();
        if (total == 0) {
            return new TopPathsSummary(windowMinutes, 0, 0, List.of(), List.of());
        }

        Map<String, List<RateLimitLog>> byPath = logs.stream()
                .filter(l -> l.getRequestPath() != null)
                .collect(Collectors.groupingBy(RateLimitLog::getRequestPath));

        List<PathStat> allStats = byPath.entrySet().stream()
                .map(e -> toPathStat(e.getKey(), e.getValue()))
                .toList();

        List<PathStat> byTraffic = allStats.stream()
                .sorted(Comparator.comparingInt(PathStat::total).reversed())
                .limit(TOP_PATHS_TRAFFIC)
                .toList();

        List<PathStat> byBlockRate = allStats.stream()
                .filter(p -> p.total() >= MIN_REQUESTS_FOR_BLOCK_RATE)
                .sorted(Comparator.comparingDouble(PathStat::blockRatePct).reversed())
                .limit(TOP_PATHS_BLOCK_RATE)
                .toList();

        return new TopPathsSummary(windowMinutes, total, byPath.size(), byTraffic, byBlockRate);
    }

    private List<RateLimitLog> loadLogs(Long appInfoId, int windowMinutes) {
        Instant cutoff = Instant.now().minusSeconds((long) windowMinutes * 60);
        return logRepository.findRecentLogs(appInfoId, cutoff, AGGREGATE_LOG_LIMIT);
    }

    private RecentLogEntry toRecentLogEntry(RateLimitLog log) {
        return new RecentLogEntry(
                log.getId(), log.getAppInfoId(), log.getClientIp(),
                log.getWasBlocked(), log.getReason(), log.getHttpMethod(),
                log.getRequestPath(), log.getRemainingTokens(), log.getResponseCode(),
                log.getRequestAt(), log.getRetryAfterSeconds(), log.getRedisFailed(),
                log.getIsBot(), log.getBotName(), log.getDeviceType()
        );
    }

    private int countByCodeRange(List<RateLimitLog> logs, int from, int to) {
        return (int) logs.stream()
                .filter(l -> l.getResponseCode() != null && l.getResponseCode() >= from && l.getResponseCode() <= to)
                .count();
    }

    private int countUniqueIps(List<RateLimitLog> logs) {
        return (int) logs.stream().filter(l -> l.getClientIp() != null).map(RateLimitLog::getClientIp).distinct().count();
    }

    private List<PathEntry> topErrorPaths(List<RateLimitLog> logs, int n) {
        return logs.stream()
                .filter(l -> l.getResponseCode() != null && l.getResponseCode() >= 400 && l.getRequestPath() != null)
                .collect(Collectors.groupingBy(RateLimitLog::getRequestPath, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .map(e -> new PathEntry(e.getKey(), e.getValue()))
                .toList();
    }

    private List<BlockReasonEntry> topBlockReasons(List<RateLimitLog> logs, int n) {
        return logs.stream()
                .filter(l -> Boolean.TRUE.equals(l.getWasBlocked()) && l.getReason() != null)
                .collect(Collectors.groupingBy(RateLimitLog::getReason, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .map(e -> new BlockReasonEntry(e.getKey(), e.getValue()))
                .toList();
    }

    private List<IpBlockEntry> topBlockingIps(List<RateLimitLog> logs, int n) {
        return logs.stream()
                .filter(l -> Boolean.TRUE.equals(l.getWasBlocked()) && l.getClientIp() != null)
                .collect(Collectors.groupingBy(RateLimitLog::getClientIp, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .map(e -> new IpBlockEntry(e.getKey(), e.getValue()))
                .toList();
    }

    private double computeIpConcentration(List<IpBlockEntry> topBlockingIps, int totalBlocked) {
        if (totalBlocked == 0 || topBlockingIps.isEmpty()) {
            return 0.0;
        }
        return pct(topBlockingIps.getFirst().blocks(), totalBlocked);
    }

    private List<PathAvgEntry> topTokenConsumingPaths(List<RateLimitLog> logs, int n) {
        return logs.stream()
                .filter(l -> l.getRequestPath() != null)
                .collect(Collectors.groupingBy(RateLimitLog::getRequestPath,
                        Collectors.averagingLong(RateLimitLog::getRemainingTokens)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(n)
                .map(e -> new PathAvgEntry(e.getKey(), e.getValue()))
                .toList();
    }

    private int countIpsNearDepletion(List<RateLimitLog> logs, long threshold) {
        return (int) logs.stream()
                .filter(l -> l.getClientIp() != null && l.getRemainingTokens() <= threshold)
                .map(RateLimitLog::getClientIp)
                .distinct()
                .count();
    }

    private int countIpsDepleted(List<RateLimitLog> logs) {
        return (int) logs.stream()
                .filter(l -> l.getClientIp() != null && l.getRemainingTokens() == 0)
                .map(RateLimitLog::getClientIp)
                .distinct()
                .count();
    }

    private PathStat toPathStat(String path, List<RateLimitLog> logs) {
        int total = logs.size();
        int blocked = (int) logs.stream().filter(l -> Boolean.TRUE.equals(l.getWasBlocked())).count();
        String topMethod = logs.stream()
                .filter(l -> l.getHttpMethod() != null)
                .collect(Collectors.groupingBy(RateLimitLog::getHttpMethod, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        List<IpBlockEntry> topIps = topBlockingIps(logs, TOP_BLOCKING_IPS_PER_PATH);
        return new PathStat(path, total, blocked, pct(blocked, total), topMethod, topIps);
    }

    private double pct(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : Math.round((numerator * 100.0 / denominator) * 100.0) / 100.0;
    }

    private ErrorSummary emptyErrorSummary(int windowMinutes) {
        return new ErrorSummary(windowMinutes, 0, 0, 0, 0, 0, 0, 0, 0,
                List.of(), List.of(), 0, List.of(), 0, 0);
    }

    private TokenHealthSummary emptyTokenHealthSummary(int windowMinutes, int total) {
        return new TokenHealthSummary(windowMinutes, total, 0, 0, 0, 0, 0, 0,
                List.of(), 0, 0, 0);
    }
}
