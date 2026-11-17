package com.hzk.loganomaly.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class AccessLogDTO {

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
     * 源 IP
     */
    private String ip;

    /**
     * 状态码
     */
    private Integer statusCode;

    /**
     * 请求方法（GET / POST）
     */
    private String method;

    /**
     * IP 归属地关键字
     */
    private String ipLocation;

    /**
     * 是否异常（0-正常，1-异常）
     */
    private Integer finalAbnormal;

    /**
     * 开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}

