package com.app.ratelimiter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register or update an application")
public record AppRequest(
        @Schema(description = "Unique application identifier", example = "payments-service")
        @NotBlank @Size(max = 255) String appId,

        @Schema(description = "Port the application runs on — must be unique across all apps", example = "8081")
        @NotNull @Min(1) @Max(65535) Integer port,

        @Schema(description = "Optional description of the application", example = "Handles all payment transactions")
        @Size(max = 1000) String description
) {}
