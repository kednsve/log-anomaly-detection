package com.hzk.loganomaly.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "my-config.detect-rules")
public class DetectRuleConfig {
    private HighFrequency highFrequency = new HighFrequency();
    private Scan scan = new Scan();
    private AuthFailure authFailure = new AuthFailure();

    @Data
    public static class HighFrequency {
        private boolean enabled = true;
        private int peakPerMinute = 60;
        private int minTotal = 60;
    }

    @Data
    public static class Scan {
        private boolean enabled = true;
        private int min404 = 10;
        private double minRatio = 0.2;
    }

    @Data
    public static class AuthFailure {
        private boolean enabled = true;
        private int minTotal = 20;
        private double minRatio = 0.5;
    }
}
