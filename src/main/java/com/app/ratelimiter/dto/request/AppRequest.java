package com.app.ratelimiter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register or update an application")
public record AppRequest(
        @Schema(description = "Unique service name identifier", example = "payments-service")
        @NotBlank @Size(max = 255) String serviceName,

        @Schema(description = "Full URL where the service is running", example = "http://personal-website:8080")
        @NotBlank @Size(max = 500) String serviceUrl,

        @Schema(description = "Optional description of the application", example = "Handles all payment transactions")
        @Size(max = 1000) String description
) {}
