package com.app.ratelimiter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to check whether an app has remaining rate limit tokens")
public record RateLimitCheckRequest(
        @Schema(description = "Service name or port number as registered in app_info", example = "payments-service or 8085")
        @NotBlank @Size(max = 255) String serviceIdentifier,

        @Schema(description = "Client IP address (IPv4 or IPv6)", example = "203.0.113.42")
        @NotBlank @Size(max = 45) @Pattern(regexp = "^[\\d.:a-fA-F]+$", message = "must be a valid IP address") String clientIp,

        @Schema(description = "Request path to match against plans. Null defaults to /**.", example = "/resources")
        @Size(max = 500) String requestPath,

        @Schema(description = "HTTP method of the incoming request. Optional, saved to audit log.", example = "POST")
        @Size(max = 10) String httpMethod,

        @Schema(description = "Trace or correlation ID from upstream (e.g. X-Request-ID header). Optional, saved to audit log.", example = "abc123def456")
        @Size(max = 64) String traceId
) {}
