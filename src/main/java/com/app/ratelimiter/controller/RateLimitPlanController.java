package com.app.ratelimiter.controller;

import com.app.ratelimiter.dto.request.RateLimitPlanRequest;
import com.app.ratelimiter.dto.response.PageResponse;
import com.app.ratelimiter.dto.response.RateLimitPlanResponse;
import com.app.ratelimiter.service.RateLimitPlanService;
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

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class RateLimitPlanController {

    private final RateLimitPlanService planService;

    @PostMapping
    public ResponseEntity<RateLimitPlanResponse> create(@Valid @RequestBody RateLimitPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<RateLimitPlanResponse>> getAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(planService.getAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RateLimitPlanResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(planService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RateLimitPlanResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RateLimitPlanRequest request) {
        return ResponseEntity.ok(planService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
