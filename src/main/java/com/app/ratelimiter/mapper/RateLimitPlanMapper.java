package com.app.ratelimiter.mapper;

import com.app.ratelimiter.dto.request.RateLimitPlanRequest;
import com.app.ratelimiter.dto.response.RateLimitPlanResponse;
import com.app.ratelimiter.model.RateLimitPlan;
import org.springframework.stereotype.Component;

@Component
public class RateLimitPlanMapper {

    private static final String DEFAULT_PATH_PATTERN = "/**";

    public RateLimitPlan toEntity(RateLimitPlanRequest request, Long appInfoId) {
        return RateLimitPlan.builder()
                .appInfoId(appInfoId)
                .pathPattern(request.pathPattern() != null ? request.pathPattern() : DEFAULT_PATH_PATTERN)
                .capacity(request.capacity())
                .refillRate(request.refillRate())
                .refillPeriodSeconds(request.refillPeriodSeconds())
                .description(request.description())
                .enabled(true)
                .build();
    }

    public RateLimitPlanResponse toResponse(RateLimitPlan entity) {
        return new RateLimitPlanResponse(
                entity.getId(),
                entity.getAppInfoId(),
                entity.getPathPattern(),
                entity.getCapacity(),
                entity.getRefillRate(),
                entity.getRefillPeriodSeconds(),
                entity.getDescription(),
                entity.getEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
