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
        private boolean enabled;
        private int peakPerMinute;
        private int minTotal;
    }

    @Data
    public static class Scan {
        private boolean enabled;
        private int min404;
        private double minRatio;
    }

    @Data
    public static class AuthFailure {
        private boolean enabled;
        private int minTotal;
        private double minRatio;
    }
}
