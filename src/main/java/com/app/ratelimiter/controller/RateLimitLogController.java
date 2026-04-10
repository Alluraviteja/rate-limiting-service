package com.app.ratelimiter.controller;

import com.app.ratelimiter.dto.response.ErrorResponse;
import com.app.ratelimiter.dto.response.PageResponse;
import com.app.ratelimiter.dto.response.RateLimitLogResponse;
import com.app.ratelimiter.service.RateLimitLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit Logs", description = "Paginated audit log of all rate limit check events. Requires ROLE_ADMIN.")
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class RateLimitLogController {

    private final RateLimitLogService logService;

    @Operation(summary = "List audit logs", description = "Returns a paginated list of rate limit check events. Optionally filter by appId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of audit log entries"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role — requires ROLE_ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<RateLimitLogResponse>> getLogs(
            @Parameter(description = "Filter logs by appId (optional)") @RequestParam(required = false) String appId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(logService.getLogs(appId, pageable)));
    }
}
