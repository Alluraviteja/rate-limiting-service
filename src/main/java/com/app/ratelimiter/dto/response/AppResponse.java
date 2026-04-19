package com.app.ratelimiter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Registered application details")
public record AppResponse(
        @Schema(description = "Unique record ID", example = "1")
        Long id,

        @Schema(description = "Unique application identifier", example = "payments-service")
        String appId,

        @Schema(description = "Port the application runs on", example = "8081")
        Integer port,

        @Schema(description = "Optional description of the application", example = "Handles all payment transactions")
        String description,

        @Schema(description = "Whether this app is active", example = "true")
        boolean enabled,

        @Schema(description = "UTC timestamp when the app was registered")
        Instant createdAt,

        @Schema(description = "UTC timestamp when the app was last updated")
        Instant updatedAt
) {}
