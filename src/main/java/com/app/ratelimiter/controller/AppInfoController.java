package com.app.ratelimiter.controller;

import com.app.ratelimiter.dto.request.AppInfoRequest;
import com.app.ratelimiter.dto.response.AppInfoResponse;
import com.app.ratelimiter.dto.response.ErrorResponse;
import com.app.ratelimiter.dto.response.PageResponse;
import com.app.ratelimiter.service.AppInfoService;
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

@Tag(name = "Apps", description = "Registry of applications. Requires ROLE_ADMIN.")
@RestController
@RequestMapping("/api/v1/apps")
@RequiredArgsConstructor
public class AppInfoController {

    private final AppInfoService appInfoService;

    @Operation(summary = "Register an app", description = "Registers a new application. The serviceName and serviceUrl must be unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "App registered successfully",
                    content = @Content(schema = @Schema(implementation = AppInfoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "App already registered with this serviceName or serviceUrl",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<AppInfoResponse> create(@Valid @RequestBody AppInfoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appInfoService.create(request));
    }

    @Operation(summary = "List all apps", description = "Returns a paginated list of all registered applications.")
    @ApiResponse(responseCode = "200", description = "Page of registered apps")
    @GetMapping
    public ResponseEntity<PageResponse<AppInfoResponse>> getAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(appInfoService.getAll(pageable)));
    }

    @Operation(summary = "Get an app by ID", description = "Returns the registered application with the given ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "App found",
                    content = @Content(schema = @Schema(implementation = AppInfoResponse.class))),
            @ApiResponse(responseCode = "404", description = "App not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AppInfoResponse> getById(
            @Parameter(description = "App record ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(appInfoService.getById(id));
    }

    @Operation(summary = "Update an app", description = "Updates the description of a registered application. The serviceName cannot be changed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "App updated successfully",
                    content = @Content(schema = @Schema(implementation = AppInfoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "App not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<AppInfoResponse> update(
            @Parameter(description = "App record ID", required = true) @PathVariable Long id,
            @Valid @RequestBody AppInfoRequest request) {
        return ResponseEntity.ok(appInfoService.update(id, request));
    }

    @Operation(summary = "Delete an app", description = "Removes an application from the registry.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "App deleted successfully"),
            @ApiResponse(responseCode = "404", description = "App not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "App record ID", required = true) @PathVariable Long id) {
        appInfoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
