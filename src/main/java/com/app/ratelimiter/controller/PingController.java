package com.app.ratelimiter.controller;

import com.app.ratelimiter.dto.response.PingResponse;
import com.app.ratelimiter.service.PingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "Service and dependency health checks")
@RestController
@RequestMapping("/api/v1/ping")
@RequiredArgsConstructor
public class PingController {

    private final PingService pingService;

    @Operation(summary = "Health check", description = "Returns connectivity status for the service, PostgreSQL, and Redis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All dependencies are reachable",
                    content = @Content(schema = @Schema(implementation = PingResponse.class))),
            @ApiResponse(responseCode = "503", description = "One or more dependencies are unreachable",
                    content = @Content(schema = @Schema(implementation = PingResponse.class)))
    })
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<PingResponse> ping() {
        PingResponse response = pingService.ping();
        HttpStatus status = "UP".equals(response.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(response);
    }
}
