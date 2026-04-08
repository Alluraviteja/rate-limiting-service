package com.app.ratelimiter.service;

import com.app.ratelimiter.dto.response.PingResponse;

public interface PingService {
    PingResponse ping();
}
