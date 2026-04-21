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
    public void log(Long appInfoId, String clientIp, boolean wasBlocked, String reason, String httpMethod) {
        RateLimitLog entry = RateLimitLog.builder()
                .appInfoId(appInfoId)
                .clientIp(clientIp)
                .wasBlocked(wasBlocked)
                .reason(reason)
                .httpMethod(httpMethod)
                .build();
        logRepository.save(entry);
        log.debug("Audit log saved: appInfoId={}, clientIp={}, blocked={}, method={}", appInfoId, clientIp, wasBlocked, httpMethod);
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
