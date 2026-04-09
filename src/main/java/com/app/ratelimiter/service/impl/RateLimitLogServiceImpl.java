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
    public void log(String appId, String clientIp, boolean wasBlocked, String reason) {
        RateLimitLog entry = RateLimitLog.builder()
                .appId(appId)
                .clientIp(clientIp)
                .wasBlocked(wasBlocked)
                .reason(reason)
                .build();
        logRepository.save(entry);
        log.debug("Audit log saved: appId={}, clientIp={}, blocked={}", appId, clientIp, wasBlocked);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RateLimitLogResponse> getLogs(String appId, Pageable pageable) {
        if (appId != null && !appId.isBlank()) {
            return logRepository.findByAppId(appId, pageable).map(mapper::toResponse);
        }
        return logRepository.findAll(pageable).map(mapper::toResponse);
    }
}
