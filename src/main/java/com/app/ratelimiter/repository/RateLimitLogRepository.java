package com.app.ratelimiter.repository;

import com.app.ratelimiter.model.RateLimitLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateLimitLogRepository extends JpaRepository<RateLimitLog, Long> {

    Page<RateLimitLog> findByAppId(String appId, Pageable pageable);
}
