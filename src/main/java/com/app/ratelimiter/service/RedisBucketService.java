package com.app.ratelimiter.service;

import io.github.bucket4j.ConsumptionProbe;

public interface RedisBucketService {
    ConsumptionProbe tryConsume(String bucketKey, ResolvedBucketConfig resolved);
}
