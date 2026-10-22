package com.hzk.loganomaly.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("upload_task")
public class UploadTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private Integer totalRows;

    private Integer successRows;

    private Integer failRows;

    private String failDetail;

    private Integer status;

    private LocalDateTime uploadTime;

    private LocalDateTime finishTime;
}
