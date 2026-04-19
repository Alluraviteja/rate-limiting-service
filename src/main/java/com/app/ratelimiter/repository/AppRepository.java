package com.app.ratelimiter.repository;

import com.app.ratelimiter.model.App;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppRepository extends JpaRepository<App, Long> {

    Optional<App> findByAppId(String appId);

    Optional<App> findByPort(Integer port);

    boolean existsByAppId(String appId);

    boolean existsByPort(Integer port);
}
