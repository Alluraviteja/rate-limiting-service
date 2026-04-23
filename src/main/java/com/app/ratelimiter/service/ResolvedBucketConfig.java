package com.app.ratelimiter.service;

import io.github.bucket4j.BucketConfiguration;

public record ResolvedBucketConfig(BucketConfiguration config, String matchedPattern, long capacity) {

    public boolean hasMatch() {
        return config != null;
    }

    public static ResolvedBucketConfig noMatch() {
        return new ResolvedBucketConfig(null, null, -1);
    }
}
