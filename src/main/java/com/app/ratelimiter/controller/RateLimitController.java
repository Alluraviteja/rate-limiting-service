package com.app.ratelimiter.controller;

import com.app.ratelimiter.dto.request.RateLimitCheckRequest;
import com.app.ratelimiter.dto.response.ErrorResponse;
import com.app.ratelimiter.dto.response.RateLimitCheckResponse;
import com.app.ratelimiter.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rate Limit", description = "Token bucket rate limit check for downstream services")
@RestController
@RequestMapping("/api/v1/ratelimit")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitService rateLimitService;

    @Operation(summary = "Check rate limit", description = "Consumes one token from the app's bucket. Returns whether the request is allowed and remaining token count. Requires ROLE_SERVICE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rate limit check result (allowed or blocked)",
                    content = @Content(schema = @Schema(implementation = RateLimitCheckResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role — requires ROLE_SERVICE",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No active rate limit plan found for the given appId",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/check")
    public ResponseEntity<RateLimitCheckResponse> check(@Valid @RequestBody RateLimitCheckRequest request) {
        return ResponseEntity.ok(rateLimitService.check(request));
    }
}
