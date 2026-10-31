package com.hzk.loganomaly.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("access_log")
public class AccessLog {

    /**
     * 主键 ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 源 IP 地址
     */
    private String ip;

    /**
     * IP 归属地（例如：中国 广东省 深圳市）
     */
    private String ipLocation;

    /**
     * 访问时间（日志发生时间）
     */
    private LocalDateTime accessTime;

    /**
     * 请求 URL 路径
     */
    private String url;

    /**
     * HTTP 状态码（如 200, 403, 404）
     */
    private Integer statusCode;

    /**
     * HTTP 请求方法（GET, POST 等）
     */
    private String method;

    /**
     * 响应时间
     */
    private Integer responseTime;

    /**
     * 用户代理（User-Agent，浏览器/客户端标识）
     */
    private String userAgent;

    /**
     * 目标端口
     */
    private Integer destPort;

    /**
     * 传输层协议（TCP, UDP 等）
     */
    private String protocol;

    /**
     * 网络流持续时间
     */
    private Long duration;

    /**
     * 源端发送字节数
     */
    private Long srcBytes;

    /**
     * 目的端发送字节数
     */
    private Long dstBytes;

    /**
     * 规则引擎检测结果：0-正常，1-异常
     */
    private Integer ruleAbnormal;

    /**
     * 命中的具体规则类型或异常描述
     */
    private String ruleType;

    /**
     * 机器学习模型（Isolation Forest）计算的异常分数
     */
    private Double modelScore;

    /**
     * 机器学习模型检测结果：0-正常，1-异常
     */
    private Integer modelAbnormal;

    /**
     * 最终综合判定结果：0-正常，1-异常
     */
    private Integer finalAbnormal;

    /**
     * 关联的上传任务 ID
     */
    private Long uploadTaskId;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;
}