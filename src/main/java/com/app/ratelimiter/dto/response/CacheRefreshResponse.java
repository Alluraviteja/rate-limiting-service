package com.app.ratelimiter.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Result of an in-memory cache refresh operation")
public record CacheRefreshResponse(
        @Schema(description = "Entries cleared from the AppInfo cache", example = "4")
        int appInfoEntriesCleared,

        @Schema(description = "Entries cleared from the plan list and bucket config caches", example = "6")
        int planEntriesCleared,

        @Schema(description = "UTC timestamp when the refresh was performed")
        Instant refreshedAt
) {}
