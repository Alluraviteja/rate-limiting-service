package com.app.ratelimiter.repository;

import com.app.ratelimiter.model.RateLimitPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RateLimitPlanRepository extends JpaRepository<RateLimitPlan, Long> {

    Optional<RateLimitPlan> findByAppId(String appId);

    Optional<RateLimitPlan> findByAppIdAndEnabledTrue(String appId);

    boolean existsByAppId(String appId);
}
