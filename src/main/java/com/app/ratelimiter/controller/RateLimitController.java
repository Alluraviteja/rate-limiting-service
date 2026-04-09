package com.app.ratelimiter.controller;

import com.app.ratelimiter.dto.request.RateLimitCheckRequest;
import com.app.ratelimiter.dto.response.RateLimitCheckResponse;
import com.app.ratelimiter.service.RateLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ratelimit")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitService rateLimitService;

    @PostMapping("/check")
    public ResponseEntity<RateLimitCheckResponse> check(@Valid @RequestBody RateLimitCheckRequest request) {
        return ResponseEntity.ok(rateLimitService.check(request));
    }
}
