package com.hzk.loganomaly.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class AlertDTO {
    /**
     * 当前页码（默认第 1 页）
     */
    private Long pageNum = 1L;

    /**
     * 每页条数（默认 10 条）
     */
    private Long pageSize = 10L;
    /**
     * id
     */
    private Long id;
    /**
     * 上传任务
     */
    private Long taskId;
    /**
     * Ip地址
     */
    private String ip;
    /**
     * 警告类型
     */
    private String alertType;
    /**
     * 告警来源：rule / model / both
     */
    private String alertSource;
    /**
     * 描述信息
     */
    private String description;
    /**
     * 告警时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTriggerTime;
    /**
     * 告警时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTriggerTime;
    /**
     * 处理状态：0-未处理，1-已处理，2-误报
     */
    private Integer status;
    /**
     * 处理时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startHandleTime;
    /**
     * 处理时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endHandleTime;
}
