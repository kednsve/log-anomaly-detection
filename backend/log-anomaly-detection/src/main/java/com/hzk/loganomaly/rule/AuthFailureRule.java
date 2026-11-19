package com.hzk.loganomaly.rule;

import com.hzk.loganomaly.config.DetectRuleConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
@RequiredArgsConstructor
public class AuthFailureRule implements Rule {
    private final DetectRuleConfig detectRuleConfig;

    @Override
    public String type() {
        return "认证失败";
    }

    /**
     * 认证失败检测
     *
     * @param ipFeature ip画像
     * @return 请求大于一定值，认证失败大于一定值
     */
    @Override
    public boolean match(IpFeature ipFeature) {
        DetectRuleConfig.AuthFailure authFailure = detectRuleConfig.getAuthFailure();
        return authFailure.isEnabled()
                && ipFeature.getTotalRequests() != null
                && ipFeature.getTotalRequests() >= authFailure.getMinTotal()
                && ipFeature.getAuthFailureRatio() >= authFailure.getMinRatio();
    }

    @Override
    public String describe(IpFeature ipFeature) {
        return String.format(
                "401/403 请求 %d 次，占比 %.0f%%",
                ipFeature.getAuthFailureCount() == null ? 0 : ipFeature.getAuthFailureCount(),
                ipFeature.getAuthFailureRatio() * 100
        );
    }
}
