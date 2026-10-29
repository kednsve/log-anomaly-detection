package com.hzk.loganomaly.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("access_log")
public class AccessLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ip;
    private String ipLocation;
    private LocalDateTime accessTime;
    private String url;
    private Integer statusCode;
    private String method;
    private Integer responseTime;
    private String userAgent;
    private Integer destPort;
    private String protocol;
    private Long duration;
    private Long srcBytes;
    private Long dstBytes;
    private Integer ruleAbnormal;
    private String ruleType;
    private Double modelScore;
    private Integer modelAbnormal;
    private Integer finalAbnormal;
    private Long uploadTaskId;
    private LocalDateTime createTime;
}