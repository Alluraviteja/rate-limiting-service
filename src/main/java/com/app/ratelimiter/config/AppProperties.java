package com.app.ratelimiter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Mcp mcp = new Mcp();

    @Getter
    @Setter
    public static class Mcp {
        private String secret = "";
    }
}
