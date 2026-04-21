package com.app.ratelimiter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Health check response indicating connectivity status of the service and its dependencies")
public record PingResponse(
        @Schema(description = "Overall service status: UP if all dependencies are reachable, DOWN otherwise", example = "UP")
        String status,

        @Schema(description = "PostgreSQL connectivity status")
        ComponentStatus database,

        @Schema(description = "Redis connectivity status")
        ComponentStatus redis,

        @Schema(description = "UTC timestamp of the health check")
        Instant timestamp
) {
    @Schema(description = "Connectivity status of a single dependency")
    public record ComponentStatus(
            @Schema(description = "Whether the dependency is reachable", example = "true")
            boolean connected,

            @Schema(description = "Human-readable connectivity message", example = "Connected")
            String message
    ) {}
}
