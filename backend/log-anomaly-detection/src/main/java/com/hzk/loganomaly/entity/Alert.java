package com.hzk.loganomaly.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert")
public class Alert {

    /**
     * 主键 ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 触发告警的源 IP 地址
     */
    private String ip;
    /**
     * 关联的上传任务 ID
     */
    private Long taskId;

    /**
     * 告警类型
     */
    private String alertType;

    /**
     * 告警来源：
     * rule - 规则引擎命中
     * model - 机器学习模型命中
     * both - 规则和模型同时命中
     */
    private String alertSource;

    /**
     * 告警详细描述信息
     */
    private String description;

    /**
     * 触发时间（告警产生的时间）
     */
    private LocalDateTime triggerTime;

    /**
     * 处理状态： 0 - 未处理 1 - 已处理 2 - 误报
     */
    private Integer status;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 处理备注
     */
    private String handleRemark;

}
