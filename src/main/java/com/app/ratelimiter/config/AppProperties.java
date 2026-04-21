package com.app.ratelimiter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Ratelimit ratelimit = new Ratelimit();

    @Getter
    @Setter
    public static class Ratelimit {
        private Redis redis = new Redis();

        @Getter
        @Setter
        public static class Redis {
            private FailureStrategy failureStrategy = FailureStrategy.FAIL_OPEN;
        }
    }
}
