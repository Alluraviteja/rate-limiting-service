package com.app.ratelimiter.service.impl;

import com.app.ratelimiter.service.RedisBucketService;
import com.app.ratelimiter.service.ResolvedBucketConfig;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RedisBucketServiceImpl implements RedisBucketService {

    private final LettuceBasedProxyManager<String> proxyManager;

    public RedisBucketServiceImpl(@Lazy LettuceBasedProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @CircuitBreaker(name = "redis")
    @Retry(name = "redis")
    @Override
    public ConsumptionProbe tryConsume(String bucketKey, ResolvedBucketConfig resolved) {
        BucketProxy bucket = proxyManager.builder().build(bucketKey, resolved::config);
        return bucket.tryConsumeAndReturnRemaining(1);
    }
}
