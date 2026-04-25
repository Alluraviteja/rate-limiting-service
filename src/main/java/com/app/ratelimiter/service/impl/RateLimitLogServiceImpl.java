package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.dto.response.RateLimitLogResponse;
import com.app.ratelimiter.mapper.RateLimitLogMapper;
import com.app.ratelimiter.model.RateLimitLog;
import com.app.ratelimiter.repository.RateLimitLogRepository;
import com.app.ratelimiter.service.RateLimitAuditEntry;
import com.app.ratelimiter.service.RateLimitLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitLogServiceImpl implements RateLimitLogService {

    private final RateLimitLogRepository logRepository;
    private final RateLimitLogMapper mapper;

    @Override
    @Transactional
    public void log(RateLimitAuditEntry entry) {
        RateLimitLog record = RateLimitLog.builder()
                .appInfoId(entry.getAppInfoId())
                .clientIp(entry.getClientIp())
                .wasBlocked(entry.isWasBlocked())
                .reason(entry.getReason())
                .httpMethod(entry.getHttpMethod())
                .requestPath(entry.getRequestPath())
                .remainingTokens(entry.getRemainingTokens())
                .traceId(entry.getTraceId())
                .responseCode(entry.getResponseCode())
                .requestAt(entry.getRequestAt())
                .retryAfterSeconds(entry.getRetryAfterSeconds())
                .redisFailed(entry.isRedisFailed())
                .browser(entry.getBrowser())
                .os(entry.getOs())
                .deviceType(entry.getDeviceType())
                .isBot(entry.isBot())
                .botName(entry.getBotName())
                .requestSize(entry.getRequestSize())
                .referer(entry.getReferer())
                .build();
        logRepository.save(record);
        log.debug("Audit log saved: appInfoId={}, clientIp={}, blocked={}, responseCode={}, path={}, remaining={}, retryAfter={}s, redisFailed={}, deviceType={}, isBot={}",
                entry.getAppInfoId(), entry.getClientIp(), entry.isWasBlocked(), entry.getResponseCode(),
                entry.getRequestPath(), entry.getRemainingTokens(), entry.getRetryAfterSeconds(),
                entry.isRedisFailed(), entry.getDeviceType(), entry.isBot());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RateLimitLogResponse> getLogs(Long appInfoId, Pageable pageable) {
        if (appInfoId != null) {
            return logRepository.findByAppInfoId(appInfoId, pageable).map(mapper::toResponse);
        }
        return logRepository.findAll(pageable).map(mapper::toResponse);
    }
}
