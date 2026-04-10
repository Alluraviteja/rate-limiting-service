package com.app.ratelimiter.controller;

import com.app.ratelimiter.dto.request.RateLimitPlanRequest;
import com.app.ratelimiter.dto.response.ErrorResponse;
import com.app.ratelimiter.dto.response.PageResponse;
import com.app.ratelimiter.dto.response.RateLimitPlanResponse;
import com.app.ratelimiter.service.RateLimitPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rate Limit Plans", description = "CRUD management of per-app rate limit plans. Requires ROLE_ADMIN.")
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class RateLimitPlanController {

    private final RateLimitPlanService planService;

    @Operation(summary = "Create a plan", description = "Creates a new rate limit plan for an app. appId must be unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plan created successfully",
                    content = @Content(schema = @Schema(implementation = RateLimitPlanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role — requires ROLE_ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "A plan already exists for the given appId",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<RateLimitPlanResponse> create(@Valid @RequestBody RateLimitPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(request));
    }

    @Operation(summary = "List all plans", description = "Returns a paginated list of all rate limit plans.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of rate limit plans"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role — requires ROLE_ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<RateLimitPlanResponse>> getAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(planService.getAll(pageable)));
    }

    @Operation(summary = "Get a plan by ID", description = "Returns the rate limit plan with the given ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan found",
                    content = @Content(schema = @Schema(implementation = RateLimitPlanResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role — requires ROLE_ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RateLimitPlanResponse> getById(
            @Parameter(description = "Plan ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(planService.getById(id));
    }

    @Operation(summary = "Update a plan", description = "Replaces the configuration of an existing rate limit plan. The updated bucket configuration takes effect on the next request after the current Redis TTL expires.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan updated successfully",
                    content = @Content(schema = @Schema(implementation = RateLimitPlanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role — requires ROLE_ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<RateLimitPlanResponse> update(
            @Parameter(description = "Plan ID", required = true) @PathVariable Long id,
            @Valid @RequestBody RateLimitPlanRequest request) {
        return ResponseEntity.ok(planService.update(id, request));
    }

    @Operation(summary = "Delete a plan", description = "Permanently deletes a rate limit plan. The associated Redis bucket key will expire naturally based on its TTL.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plan deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role — requires ROLE_ADMIN",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Plan not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Plan ID", required = true) @PathVariable Long id) {
        planService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
