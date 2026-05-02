package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.dto.request.RateLimitPlanRequest;
import com.app.ratelimiter.dto.response.RateLimitPlanResponse;
import com.app.ratelimiter.exception.ResourceAlreadyExistsException;
import com.app.ratelimiter.exception.ResourceNotFoundException;
import com.app.ratelimiter.mapper.RateLimitPlanMapper;
import com.app.ratelimiter.model.AppInfo;
import com.app.ratelimiter.model.RateLimitPlan;
import com.app.ratelimiter.repository.AppInfoRepository;
import com.app.ratelimiter.repository.RateLimitPlanRepository;
import com.app.ratelimiter.service.RateLimitPlanService;
import com.app.ratelimiter.service.ResolvedBucketConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitPlanServiceImpl implements RateLimitPlanService {

    private static final String DEFAULT_PATH_PATTERN = "/**";

    private final RateLimitPlanRepository planRepository;
    private final AppInfoRepository appInfoRepository;
    private final RateLimitPlanMapper mapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final ConcurrentHashMap<Long, List<RateLimitPlan>> planListCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BucketConfiguration> bucketConfigCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public RateLimitPlanResponse create(RateLimitPlanRequest request) {
        AppInfo app = findByServiceNameOrPort(request.serviceName())
                .orElseThrow(() -> new ResourceNotFoundException("App not found with serviceName: " + request.serviceName()));
        String pathPattern = request.pathPattern() != null ? request.pathPattern() : DEFAULT_PATH_PATTERN;
        if (planRepository.existsByAppInfoIdAndPathPattern(app.getId(), pathPattern)) {
            throw new ResourceAlreadyExistsException(
                    "Plan already exists for serviceName: " + request.serviceName() + " and pathPattern: " + pathPattern);
        }
        RateLimitPlan saved = planRepository.save(mapper.toEntity(request, app.getId()));
        planListCache.remove(app.getId());
        log.info("Plan created for serviceName={}, appInfoId={}, pathPattern={}", request.serviceName(), app.getId(), pathPattern);
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
        evictCache(plan.getAppInfoId(), plan.getPathPattern());
        plan.setCapacity(request.capacity());
        plan.setRefillRate(request.refillRate());
        plan.setRefillPeriodSeconds(request.refillPeriodSeconds());
        plan.setDescription(request.description());
        log.info("Plan updated for appInfoId={}, pathPattern={}", plan.getAppInfoId(), plan.getPathPattern());
        return mapper.toResponse(planRepository.save(plan));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RateLimitPlan plan = findById(id);
        evictCache(plan.getAppInfoId(), plan.getPathPattern());
        planRepository.delete(plan);
        log.info("Plan deleted for appInfoId={}, pathPattern={}", plan.getAppInfoId(), plan.getPathPattern());
    }

    @Override
    @Transactional(readOnly = true)
    public ResolvedBucketConfig getBucketConfiguration(Long appInfoId, String requestPath) {
        String path = requestPath;
        List<RateLimitPlan> plans = planListCache.computeIfAbsent(appInfoId,
                key -> planRepository.findAllByAppInfoIdAndEnabledTrue(key));

        if (plans.isEmpty()) {
            log.debug("No active plans for appInfoId={}, allowing request", appInfoId);
            return ResolvedBucketConfig.noMatch();
        }

        Comparator<String> specificity = pathMatcher.getPatternComparator(path);
        RateLimitPlan matched = plans.stream()
                .filter(p -> pathMatcher.match(p.getPathPattern(), path))
                .min((a, b) -> specificity.compare(a.getPathPattern(), b.getPathPattern()))
                .orElse(null);

        if (matched == null) {
            log.debug("No matching plan for appInfoId={}, path={}, allowing request", appInfoId, path);
            return ResolvedBucketConfig.noMatch();
        }

        String cacheKey = appInfoId + ":" + matched.getPathPattern();
        BucketConfiguration config = bucketConfigCache.computeIfAbsent(cacheKey, k -> buildConfig(matched));
        return new ResolvedBucketConfig(config, matched.getPathPattern(), matched.getCapacity());
    }

    private Optional<AppInfo> findByServiceNameOrPort(String identifier) {
        Optional<AppInfo> byName = appInfoRepository.findByServiceName(identifier);
        if (byName.isPresent()) {
            return byName;
        }
        try {
            return appInfoRepository.findByServicePort(Integer.parseInt(identifier));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private RateLimitPlan findById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));
    }

    private BucketConfiguration buildConfig(RateLimitPlan plan) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(plan.getCapacity())
                        .refillGreedy(plan.getRefillRate(), Duration.ofSeconds(plan.getRefillPeriodSeconds()))
                        .build())
                .build();
    }

    @Override
    public int evictAllPlanCaches() {
        int total = planListCache.size() + bucketConfigCache.size();
        planListCache.clear();
        bucketConfigCache.clear();
        log.info("Plan caches cleared: {} entries removed", total);
        return total;
    }

    private void evictCache(Long appInfoId, String pathPattern) {
        planListCache.remove(appInfoId);
        bucketConfigCache.remove(appInfoId + ":" + pathPattern);
    }
}
