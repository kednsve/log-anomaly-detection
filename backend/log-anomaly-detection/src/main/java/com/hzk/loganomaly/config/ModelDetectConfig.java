package com.hzk.loganomaly.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "my-config.model-detect")
public class ModelDetectConfig {
    private boolean enabled;
    private String baseUrl;
    private int connectTimeoutMs;
    private int readTimeoutMs;
}
