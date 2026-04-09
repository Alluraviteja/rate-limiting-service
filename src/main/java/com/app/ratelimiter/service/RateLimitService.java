package com.app.ratelimiter.service;

import com.app.ratelimiter.dto.request.RateLimitCheckRequest;
import com.app.ratelimiter.dto.response.RateLimitCheckResponse;

public interface RateLimitService {

    RateLimitCheckResponse check(RateLimitCheckRequest request);
}
