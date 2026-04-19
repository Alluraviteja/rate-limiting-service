package com.app.ratelimiter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to check whether an app has remaining rate limit tokens")
public record RateLimitCheckRequest(
        @Schema(description = "Port of the application as registered in app_info", example = "8081")
        @NotNull @Min(1) @Max(65535) Integer appPort,

        @Schema(description = "Client IP address (IPv4 or IPv6)", example = "203.0.113.42")
        @NotBlank @Size(max = 45) @Pattern(regexp = "^[\\d.:a-fA-F]+$", message = "must be a valid IP address") String clientIp,

        @Schema(description = "Request path to match against plans. Null defaults to /**.", example = "/resources")
        @Size(max = 500) String requestPath,

        @Schema(description = "HTTP method of the incoming request. Optional, saved to audit log.", example = "POST")
        @Size(max = 10) String httpMethod
) {}
