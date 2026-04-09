package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.dto.request.RateLimitPlanRequest;
import com.app.ratelimiter.dto.response.RateLimitPlanResponse;
import com.app.ratelimiter.exception.ResourceAlreadyExistsException;
import com.app.ratelimiter.exception.ResourceNotFoundException;
import com.app.ratelimiter.mapper.RateLimitPlanMapper;
import com.app.ratelimiter.model.RateLimitPlan;
import com.app.ratelimiter.repository.RateLimitPlanRepository;
import com.app.ratelimiter.service.RateLimitPlanService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitPlanServiceImpl implements RateLimitPlanService {

    private final RateLimitPlanRepository planRepository;
    private final RateLimitPlanMapper mapper;

    private final ConcurrentHashMap<String, BucketConfiguration> bucketConfigCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public RateLimitPlanResponse create(RateLimitPlanRequest request) {
        if (planRepository.existsByAppId(request.appId())) {
            throw new ResourceAlreadyExistsException("Plan already exists for appId: " + request.appId());
        }
        RateLimitPlan saved = planRepository.save(mapper.toEntity(request));
        log.info("Plan created for appId={}", saved.getAppId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RateLimitPlanResponse getById(Long id) {
        return mapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RateLimitPlanResponse> getAll(Pageable pageable) {
        return planRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public RateLimitPlanResponse update(Long id, RateLimitPlanRequest request) {
        RateLimitPlan plan = findById(id);
        plan.setCapacity(request.capacity());
        plan.setRefillRate(request.refillRate());
        plan.setRefillPeriodSeconds(request.refillPeriodSeconds());
        plan.setDescription(request.description());
        bucketConfigCache.remove(plan.getAppId());
        log.info("Plan updated for appId={}", plan.getAppId());
        return mapper.toResponse(planRepository.save(plan));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RateLimitPlan plan = findById(id);
        bucketConfigCache.remove(plan.getAppId());
        planRepository.delete(plan);
        log.info("Plan deleted for appId={}", plan.getAppId());
    }

    @Override
    @Transactional(readOnly = true)
    public BucketConfiguration getBucketConfiguration(String appId) {
        return bucketConfigCache.computeIfAbsent(appId, this::buildBucketConfiguration);
    }

    private RateLimitPlan findById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
    }

    private BucketConfiguration buildBucketConfiguration(String appId) {
        RateLimitPlan plan = planRepository.findByAppIdAndEnabledTrue(appId)
                .orElseThrow(() -> new ResourceNotFoundException("No active plan for appId: " + appId));
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(plan.getCapacity())
                        .refillGreedy(plan.getRefillRate(), Duration.ofSeconds(plan.getRefillPeriodSeconds()))
                        .build())
                .build();
    }
}
