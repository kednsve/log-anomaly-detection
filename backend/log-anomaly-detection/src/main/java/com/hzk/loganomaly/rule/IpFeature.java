package com.hzk.loganomaly.rule;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpFeature {
    /**
     * ip
     */
    private String ip;
    /**
     * 总请求
     */
    private Long totalRequests;
    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;
    /**
     * 404计数
     */
    private Long count404;
    /**
     * 认证失败401/403计数
     */
    private Long authFailureCount;
    /**
     * 每分钟请求
     */
    private Long peakPerMinute;

    /** 404 占比 */
    public double getRatio404() {
        if (totalRequests == null || totalRequests == 0) return 0;
        return (double) (count404 == null ? 0 : count404) / totalRequests;
    }

    /** 401/403 占比 */
    public double getAuthFailureRatio() {
        if (totalRequests == null || totalRequests == 0) return 0;
        return (double) (authFailureCount == null ? 0 : authFailureCount) / totalRequests;
    }
}
