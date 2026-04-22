package com.app.ratelimiter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register or update an application")
public record AppInfoRequest(
        @Schema(description = "Unique service name identifier", example = "payments-service")
        @NotBlank @Size(max = 255) String serviceName,

        @Schema(description = "Full URL where the service is running", example = "http://personal-website:8080")
        @NotBlank @Size(max = 500) String serviceUrl,

        @Schema(description = "Port the service is running on", example = "8080")
        @Min(1) @Max(65535) Integer servicePort,

        @Schema(description = "Optional description of the application", example = "Handles all payment transactions")
        @Size(max = 1000) String description
) {}
