package com.app.ratelimiter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Registered application details")
public record AppInfoResponse(
        @Schema(description = "Unique record ID", example = "1")
        Long id,

        @Schema(description = "Unique service name identifier", example = "payments-service")
        String serviceName,

        @Schema(description = "Full URL where the service is running", example = "http://personal-website:8080")
        String serviceUrl,

        @Schema(description = "Port the service is running on", example = "8080")
        Integer servicePort,

        @Schema(description = "Optional description of the application", example = "Handles all payment transactions")
        String description,

        @Schema(description = "Whether this app is active", example = "true")
        boolean enabled,

        @Schema(description = "UTC timestamp when the app was registered")
        Instant createdAt,

        @Schema(description = "UTC timestamp when the app was last updated")
        Instant updatedAt
) {}
