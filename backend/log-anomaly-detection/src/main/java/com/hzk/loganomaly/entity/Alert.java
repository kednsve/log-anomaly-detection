package com.hzk.loganomaly.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert")
public class Alert {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ip;

    private String alertType;

    private String alertSource;

    private String description;

    private LocalDateTime triggerTime;

    private Integer status;

    private LocalDateTime handleTime;

    private String handleRemark;

    private LocalDateTime createTime;
}
