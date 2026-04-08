package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.dto.response.PingResponse;
import com.app.ratelimiter.dto.response.PingResponse.ComponentStatus;
import com.app.ratelimiter.service.PingService;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PingServiceImpl implements PingService {

    private final DataSource dataSource;
    private final StatefulRedisConnection<String, byte[]> bucketRedisConnection;

    @Override
    public PingResponse ping() {
        ComponentStatus dbStatus = checkDatabase();
        ComponentStatus redisStatus = checkRedis();

        String overallStatus = (dbStatus.connected() && redisStatus.connected()) ? "UP" : "DEGRADED";

        return new PingResponse(overallStatus, dbStatus, redisStatus, Instant.now());
    }

    private ComponentStatus checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            return new ComponentStatus(valid, valid ? "Connected" : "Connection invalid");
        } catch (Exception e) {
            log.error("Database ping failed: {}", e.getMessage());
            return new ComponentStatus(false, "Unreachable: " + e.getMessage());
        }
    }

    private ComponentStatus checkRedis() {
        try {
            String response = bucketRedisConnection.sync().ping();
            boolean connected = "PONG".equalsIgnoreCase(response);
            return new ComponentStatus(connected, connected ? "Connected" : "Unexpected response: " + response);
        } catch (Exception e) {
            log.error("Redis ping failed: {}", e.getMessage());
            return new ComponentStatus(false, "Unreachable: " + e.getMessage());
        }
    }
}
