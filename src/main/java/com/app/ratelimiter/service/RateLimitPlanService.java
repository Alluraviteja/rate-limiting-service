package com.app.ratelimiter.service;

import com.app.ratelimiter.dto.request.RateLimitPlanRequest;
import com.app.ratelimiter.dto.response.RateLimitPlanResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RateLimitPlanService {

    RateLimitPlanResponse create(RateLimitPlanRequest request);

    RateLimitPlanResponse getById(Long id);

    Page<RateLimitPlanResponse> getAll(Pageable pageable);

    RateLimitPlanResponse update(Long id, RateLimitPlanRequest request);

    void delete(Long id);

    ResolvedBucketConfig getBucketConfiguration(Long appInfoId, String requestPath);

    int evictAllPlanCaches();
}
