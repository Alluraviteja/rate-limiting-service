package com.app.ratelimiter.service;

import com.app.ratelimiter.dto.response.RateLimitLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;

public interface RateLimitLogService {

    void log(Long appInfoId, String clientIp, boolean wasBlocked, String reason, String httpMethod,
             String requestPath, Long remainingTokens, String traceId,
             int responseCode, Instant requestAt, Long retryAfterSeconds, boolean redisFailed);

    Page<RateLimitLogResponse> getLogs(Long appInfoId, Pageable pageable);
}
