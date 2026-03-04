package com.app.ratelimiter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rate_limit_plan")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitPlan extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app_id", nullable = false, unique = true)
    private String appId;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "refill_rate", nullable = false)
    private Integer refillRate;

    @Column(name = "refill_period_seconds", nullable = false)
    private Integer refillPeriodSeconds;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
}
