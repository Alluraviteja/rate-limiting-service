package com.app.ratelimiter.controller;

import com.app.ratelimiter.dto.response.CacheRefreshResponse;
import com.app.ratelimiter.dto.response.ErrorResponse;
import com.app.ratelimiter.service.CacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cache", description = "In-memory cache management. Requires ROLE_ADMIN.")
@RestController
@RequestMapping("/api/v1/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @Operation(
            summary = "Refresh all in-memory caches",
            description = "Clears the AppInfo cache, plan list cache, and bucket config cache. " +
                    "All caches are repopulated lazily on the next request. " +
                    "Use this after manual DB changes or when stale data is suspected."
    )
    @ApiResponse(responseCode = "200", description = "Caches cleared successfully",
            content = @Content(schema = @Schema(implementation = CacheRefreshResponse.class)))
    @ApiResponse(responseCode = "403", description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/refresh")
    public ResponseEntity<CacheRefreshResponse> refreshAll() {
        return ResponseEntity.ok(cacheService.refreshAll());
    }
}
