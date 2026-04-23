package com.app.ratelimiter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_info")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppInfo extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false, unique = true, length = 255)
    private String serviceName;

    @Column(name = "service_port")
    private Integer servicePort;

    @Column(name = "description", length = 1000)
    private String description;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;
}
