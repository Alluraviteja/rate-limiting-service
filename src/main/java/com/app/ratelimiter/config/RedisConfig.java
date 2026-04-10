package com.app.ratelimiter.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final DataRedisProperties redisProperties;

    /**
     * Dedicated Lettuce client for Bucket4j.
     * Separate from Spring Data Redis's auto-configured client so that
     * Bucket4j uses its own connection with byte[] codec (required for
     * serialising bucket state).
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient lettuceRedisClient() {
        Duration timeout = redisProperties.getConnectTimeout() != null
                ? redisProperties.getConnectTimeout()
                : Duration.ofMillis(2000);
        RedisURI uri = RedisURI.builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort())
                .withTimeout(timeout)
                .build();
        log.info("Initializing Bucket4j Lettuce RedisClient → {}:{}", redisProperties.getHost(), redisProperties.getPort());
        return RedisClient.create(uri);
    }

    /**
     * Single shared connection using String keys and byte[] values.
     * Lettuce connections are thread-safe and multiplexed — one connection
     * is sufficient for high concurrency.
     */
    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> bucketRedisConnection(RedisClient lettuceRedisClient) {
        return lettuceRedisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );
    }

    /**
     * Bucket4j proxy manager backed by Redis via Lua scripts.
     *
     * Each tryConsume() call executes an atomic Lua script that:
     *   1. GETs the serialised bucket state
     *   2. Applies refill logic and checks token availability
     *   3. SETs the updated state back with a TTL
     *
     * ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax ensures
     * the Redis key lives at least as long as it takes to fully refill the bucket,
     * preventing premature eviction of active buckets under low traffic.
     */
    @Bean
    public LettuceBasedProxyManager<String> proxyManager(
            StatefulRedisConnection<String, byte[]> bucketRedisConnection) {
        return LettuceBasedProxyManager.builderFor(bucketRedisConnection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofSeconds(1))
                )
                .build();
    }
}
