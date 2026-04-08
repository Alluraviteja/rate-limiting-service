package com.app.ratelimiter.controller;

import com.app.ratelimiter.dto.response.PingResponse;
import com.app.ratelimiter.service.PingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ping")
@RequiredArgsConstructor
public class PingController {

    private final PingService pingService;

    @GetMapping
    public ResponseEntity<PingResponse> ping() {
        PingResponse response = pingService.ping();
        HttpStatus status = "UP".equals(response.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(response);
    }
}
