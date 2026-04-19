package com.app.ratelimiter.service;

import io.github.bucket4j.BucketConfiguration;

public record ResolvedBucketConfig(BucketConfiguration config, String matchedPattern) {

    public boolean hasMatch() {
        return config != null;
    }

    public static ResolvedBucketConfig noMatch() {
        return new ResolvedBucketConfig(null, null);
    }
}
