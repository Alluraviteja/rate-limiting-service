package com.app.ratelimiter.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RateLimitCheckRequest(
        @NotBlank @Size(max = 255) String appId,
        @NotBlank @Size(max = 45) @Pattern(regexp = "^[\\d.:a-fA-F]+$", message = "must be a valid IP address") String clientIp
) {}
