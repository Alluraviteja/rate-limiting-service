package com.app.ratelimiter.repository;

import com.app.ratelimiter.model.RateLimitPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RateLimitPlanRepository extends JpaRepository<RateLimitPlan, Long> {

    List<RateLimitPlan> findAllByAppInfoIdAndEnabledTrue(Long appInfoId);

    boolean existsByAppInfoIdAndPathPattern(Long appInfoId, String pathPattern);
}
