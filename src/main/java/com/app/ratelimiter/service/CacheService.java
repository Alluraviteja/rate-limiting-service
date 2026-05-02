package com.app.ratelimiter.service;

import com.app.ratelimiter.dto.response.CacheRefreshResponse;

public interface CacheService {
    CacheRefreshResponse refreshAll();
}
