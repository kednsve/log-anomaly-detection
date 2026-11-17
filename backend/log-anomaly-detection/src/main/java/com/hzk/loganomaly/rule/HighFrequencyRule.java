package com.hzk.loganomaly.rule;

import com.hzk.loganomaly.config.DetectRuleConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@RequiredArgsConstructor
public class HighFrequencyRule implements Rule {
    private final DetectRuleConfig detectRuleConfig;

    @Override
    public String type() {
        return "高频访问";
    }

    /**
     * 高频访问判断
     *
     * @param ipFeature ip画像
     * @return 每分钟访问大于一定值，总访问大于一定值
     */
    @Override
    public boolean match(IpFeature ipFeature) {
        DetectRuleConfig.HighFrequency highFrequency = detectRuleConfig.getHighFrequency();
        return highFrequency.isEnabled()
                && ipFeature.getPeakPerMinute() != null
                && ipFeature.getTotalRequests() != null
                && ipFeature.getPeakPerMinute() >= highFrequency.getPeakPerMinute()
                && ipFeature.getTotalRequests() >= highFrequency.getMinTotal();
    }

    @Override
    public String describe(IpFeature ipFeature) {
        return String.format(
                "单分钟峰值 %d 次请求，共 %d 次",
                ipFeature.getPeakPerMinute(),
                ipFeature.getTotalRequests()
        );
    }
}
