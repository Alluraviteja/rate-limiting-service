package com.app.ratelimiter.repository;

import com.app.ratelimiter.model.AppInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppInfoRepository extends JpaRepository<AppInfo, Long> {

    Optional<AppInfo> findByServiceName(String serviceName);

    Optional<AppInfo> findByServicePort(Integer servicePort);

    boolean existsByServiceName(String serviceName);

    boolean existsByServiceUrl(String serviceUrl);
}
