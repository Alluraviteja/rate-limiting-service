package com.app.ratelimiter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to check whether an app has remaining rate limit tokens")
public record RateLimitCheckRequest(
        @Schema(description = "Unique application identifier", example = "payments-service")
        @NotBlank @Size(max = 255) String appId,

        @Schema(description = "Client IP address (IPv4 or IPv6)", example = "203.0.113.42")
        @NotBlank @Size(max = 45) @Pattern(regexp = "^[\\d.:a-fA-F]+$", message = "must be a valid IP address") String clientIp
) {}
