package com.hzk.loganomaly.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DetectResultDTO {
    /** 任务 ID */
    private Long taskId;

    /** 本次任务的 IP 总数 */
    private int ipCount;

    /** 命中规则的 IP 数 */
    private int abnormalIpCount;

    /** 各规则命中的 IP 数 */
    private Map<String, Integer> ruleHitCounts;

    /** 生成的告警数 */
    private int alertCount;

    /** 检测耗时（毫秒） */
    private long elapsedMs;
}
