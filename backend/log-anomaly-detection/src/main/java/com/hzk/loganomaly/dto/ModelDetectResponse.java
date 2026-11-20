package com.hzk.loganomaly.dto;

import lombok.Data;

import java.util.List;

/**
 * Python 服务 POST /detect 的响应
 */
@Data
public class ModelDetectResponse {
    /**
     * 任务ID
     */
    private Long taskId;
    /**
     * IP数
     */
    private int ipCount;
    /**
     * 异常IP数
     */
    private int abnormalIpCount;
    /**
     * 耗时
     */
    private long elapsedMs;

    private List<IpScore> scores;

    @Data
    public static class IpScore {
        /**
         * IP
         */
        private String ip;
        /**
         * 模型判断的分数
         */
        private double score;
        /**
         * 是否异常
          */
        private Boolean isAbnormal;
        /**
         * 最后访问时间
         */
        private String lastAccessTime;
    }
}