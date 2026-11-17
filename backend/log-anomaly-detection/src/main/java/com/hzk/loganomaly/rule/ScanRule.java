package com.hzk.loganomaly.rule;

import com.hzk.loganomaly.config.DetectRuleConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
public class ScanRule implements Rule {
    private final DetectRuleConfig detectRuleConfig;

    @Override
    public String type() {
        return "大量404";
    }

    /**
     * 大量扫描判断
     *
     * @param ipFeature ip画像
     * @return 404访问量大于一定值，404占比大于一定值
     */
    @Override
    public boolean match(IpFeature ipFeature) {
        DetectRuleConfig.Scan scan = detectRuleConfig.getScan();
        return scan.isEnabled()
                && ipFeature.getTotalRequests() != null
                && ipFeature.getTotalRequests() > 0
                && ipFeature.getCount404() != null
                && ipFeature.getCount404() >= scan.getMin404()
                && ipFeature.getRatio404() >= scan.getMinRatio();
    }

    @Override
    public String describe(IpFeature ipFeature) {
        return String.format(
                "404 请求 %d 次，占比 %.0f%%",
                ipFeature.getCount404(),
                ipFeature.getRatio404() * 100
        );
    }
}
