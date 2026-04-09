package com.app.ratelimiter.controller;

import com.app.ratelimiter.dto.response.PageResponse;
import com.app.ratelimiter.dto.response.RateLimitLogResponse;
import com.app.ratelimiter.service.RateLimitLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class RateLimitLogController {

    private final RateLimitLogService logService;

    @GetMapping
    public ResponseEntity<PageResponse<RateLimitLogResponse>> getLogs(
            @RequestParam(required = false) String appId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(logService.getLogs(appId, pageable)));
    }
}
