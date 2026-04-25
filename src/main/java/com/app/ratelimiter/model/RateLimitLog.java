package com.app.ratelimiter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rate_limit_log", indexes = {
        @Index(name = "idx_client_ip", columnList = "client_ip"),
        @Index(name = "idx_app_info_id", columnList = "app_info_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app_info_id")
    private Long appInfoId;

    @Column(name = "client_ip", nullable = false, length = 45)
    private String clientIp;

    @Column(name = "was_blocked", nullable = false)
    private Boolean wasBlocked;

    @Column(name = "reason")
    private String reason;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    @Column(name = "remaining_tokens")
    private Long remainingTokens;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "request_at")
    private Instant requestAt;

    @Column(name = "retry_after_seconds")
    private Long retryAfterSeconds;

    @Column(name = "redis_failed", nullable = false)
    private Boolean redisFailed;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "browser_version", length = 20)
    private String browserVersion;

    @Column(name = "os", length = 50)
    private String os;

    @Column(name = "os_version", length = 20)
    private String osVersion;

    @Column(name = "device_type", length = 10)
    private String deviceType;

    @Column(name = "is_bot", nullable = false)
    private Boolean isBot;

    @Column(name = "bot_name", length = 100)
    private String botName;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "request_size")
    private Long requestSize;

    @Column(name = "referer", length = 2048)
    private String referer;
}
