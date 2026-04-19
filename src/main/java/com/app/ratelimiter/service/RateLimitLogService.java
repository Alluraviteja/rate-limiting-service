package com.app.ratelimiter.service;

import com.app.ratelimiter.dto.response.RateLimitLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RateLimitLogService {

    void log(Long appInfoId, String clientIp, boolean wasBlocked, String reason, String httpMethod);

    Page<RateLimitLogResponse> getLogs(Long appInfoId, Pageable pageable);
}
