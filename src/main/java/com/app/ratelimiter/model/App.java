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
public class App extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app_id", nullable = false, unique = true, length = 255)
    private String appId;

    @Column(name = "port", nullable = false, unique = true)
    private Integer port;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;
}
