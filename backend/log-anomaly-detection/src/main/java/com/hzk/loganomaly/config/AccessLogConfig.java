package com.hzk.loganomaly.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "my-config.access-log")
@Data
public class AccessLogConfig {
    private int batchSize;
}
