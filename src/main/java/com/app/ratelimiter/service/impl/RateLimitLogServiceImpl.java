package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.dto.response.RateLimitLogResponse;
import com.app.ratelimiter.mapper.RateLimitLogMapper;
import com.app.ratelimiter.model.RateLimitLog;
import com.app.ratelimiter.repository.RateLimitLogRepository;
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
    public void log(Long appInfoId, String clientIp, boolean wasBlocked, String reason, String httpMethod,
                    String requestPath, Long remainingTokens, String traceId,
                    int responseCode, java.time.Instant requestAt, Long retryAfterSeconds, boolean redisFailed) {
        RateLimitLog entry = RateLimitLog.builder()
                .appInfoId(appInfoId)
                .clientIp(clientIp)
                .wasBlocked(wasBlocked)
                .reason(reason)
                .httpMethod(httpMethod)
                .requestPath(requestPath)
                .remainingTokens(remainingTokens)
                .traceId(traceId)
                .responseCode(responseCode)
                .requestAt(requestAt)
                .retryAfterSeconds(retryAfterSeconds)
                .redisFailed(redisFailed)
                .build();
        logRepository.save(entry);
        log.debug("Audit log saved: appInfoId={}, clientIp={}, blocked={}, responseCode={}, path={}, remaining={}, retryAfter={}s, redisFailed={}",
                appInfoId, clientIp, wasBlocked, responseCode, requestPath, remainingTokens, retryAfterSeconds, redisFailed);
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
