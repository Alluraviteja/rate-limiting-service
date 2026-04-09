package com.app.ratelimiter.service;

import com.app.ratelimiter.dto.response.RateLimitLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RateLimitLogService {

    void log(String appId, String clientIp, boolean wasBlocked, String reason);

    Page<RateLimitLogResponse> getLogs(String appId, Pageable pageable);
}
