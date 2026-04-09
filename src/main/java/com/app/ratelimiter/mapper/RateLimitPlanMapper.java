package com.app.ratelimiter.mapper;

import com.app.ratelimiter.dto.request.RateLimitPlanRequest;
import com.app.ratelimiter.dto.response.RateLimitPlanResponse;
import com.app.ratelimiter.model.RateLimitPlan;
import org.springframework.stereotype.Component;

@Component
public class RateLimitPlanMapper {

    public RateLimitPlan toEntity(RateLimitPlanRequest request) {
        return RateLimitPlan.builder()
                .appId(request.appId())
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
                entity.getAppId(),
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
