package com.app.ratelimiter.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to check whether an app has remaining rate limit tokens")
public record RateLimitCheckRequest(
        @Schema(description = "Service name or port number as registered in app_info", example = "personal-website")
        @NotBlank @Size(max = 255) String serviceIdentifier,

        @Schema(description = "Client IP address (IPv4 or IPv6)", example = "1.2.3.4")
        @NotBlank @Size(max = 100) String clientIp,

        @Schema(description = "Request path to match against plans.", example = "/api/data")
        @NotBlank @Size(max = 500) String requestPath,

        @Schema(description = "HTTP method of the incoming request", example = "GET")
        @Size(max = 10) String httpMethod,

        @Schema(description = "Trace or correlation ID forwarded by the gateway", example = "abc-123-def-456")
        @Size(max = 64) String traceId,

        @Schema(description = "Device category resolved by the gateway", example = "mobile",
                allowableValues = {"mobile", "tablet", "desktop", "bot"})
        @Pattern(regexp = "^(mobile|tablet|desktop|bot)$", message = "must be one of: mobile, tablet, desktop, bot")
        String deviceType,

        @Schema(description = "Whether the request originated from a bot, as resolved by the gateway", example = "false")
        Boolean isBot,

        @Schema(description = "Bot name if identified by the gateway, empty otherwise", example = "Googlebot")
        @Size(max = 100) String botName,

        @Schema(description = "Browser name resolved by the gateway", example = "Safari")
        @Size(max = 50) String browser,

        @Schema(description = "Operating system resolved by the gateway", example = "iOS")
        @Size(max = 50) String os,

        @Schema(description = "Size of the incoming request body in bytes", example = "512")
        @PositiveOrZero Long requestSize,

        @Schema(description = "Referer header forwarded by the gateway", example = "https://google.com")
        @Size(max = 2048) String referer
) {}
