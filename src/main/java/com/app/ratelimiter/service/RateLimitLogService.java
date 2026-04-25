package com.app.ratelimiter.service;

import com.app.ratelimiter.dto.response.RateLimitLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RateLimitLogService {

    void log(RateLimitAuditEntry entry);

    Page<RateLimitLogResponse> getLogs(Long appInfoId, Pageable pageable);
}
