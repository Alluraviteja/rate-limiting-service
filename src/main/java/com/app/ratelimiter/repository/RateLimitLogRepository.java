package com.app.ratelimiter.repository;

import com.app.ratelimiter.model.RateLimitLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface RateLimitLogRepository extends JpaRepository<RateLimitLog, Long> {

    Page<RateLimitLog> findByAppInfoId(Long appInfoId, Pageable pageable);

    @Query(value = "SELECT * FROM rate_limit_log WHERE app_info_id = :appInfoId AND request_at >= :cutoff ORDER BY request_at DESC LIMIT :limit", nativeQuery = true)
    List<RateLimitLog> findRecentLogs(@Param("appInfoId") Long appInfoId, @Param("cutoff") Instant cutoff, @Param("limit") int limit);

    @Query(value = "SELECT * FROM rate_limit_log WHERE request_at >= :cutoff ORDER BY request_at DESC LIMIT :limit", nativeQuery = true)
    List<RateLimitLog> findRecentLogsAllApps(@Param("cutoff") Instant cutoff, @Param("limit") int limit);
}
