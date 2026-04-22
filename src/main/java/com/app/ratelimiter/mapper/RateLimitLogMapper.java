package com.app.ratelimiter.mapper;

import com.app.ratelimiter.dto.response.RateLimitLogResponse;
import com.app.ratelimiter.model.RateLimitLog;
import org.springframework.stereotype.Component;

@Component
public class RateLimitLogMapper {

    public RateLimitLogResponse toResponse(RateLimitLog entity) {
        return new RateLimitLogResponse(
                entity.getId(),
                entity.getAppInfoId(),
                entity.getClientIp(),
                entity.getWasBlocked(),
                entity.getReason(),
                entity.getHttpMethod(),
                entity.getRequestPath(),
                entity.getRemainingTokens(),
                entity.getTraceId(),
                entity.getResponseCode(),
                entity.getRequestAt(),
                entity.getRetryAfterSeconds(),
                entity.getRedisFailed(),
                entity.getCreatedAt()
        );
    }
}
