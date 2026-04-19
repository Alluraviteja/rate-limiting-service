package com.app.ratelimiter.repository;

import com.app.ratelimiter.model.AppInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppInfoRepository extends JpaRepository<AppInfo, Long> {

    @Query("SELECT a FROM AppInfo a WHERE a.serviceName = :identifier OR a.serviceUrl LIKE CONCAT('%:', :identifier)")
    Optional<AppInfo> findByServiceNameOrPort(@Param("identifier") String identifier);

    boolean existsByServiceName(String serviceName);

    boolean existsByServiceUrl(String serviceUrl);
}
