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
    /**
     *  文件名
     */
    private String fileName;
    /**
     * 总行数
     */
    private Integer totalRows;
    /**
     * 成功上传的行数
     */
    private Integer successRows;
    /**
     * 失败的行数
     */
    private Integer failRows;
    /**
     * 失败描述
     */
    private String failDetail;
    /**
     * 上传状态 0 处理中，1 完成，2 失败
     */
    private Integer status;
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    /**
     * 完成时间
     */
    private LocalDateTime finishTime;
    /**
     * 规则检测状态 0 未检测 1 检测中 2成功 3 失败
     */
    private Integer ruleDetect;
    /**
     * 模型检测状态 0 未检测 1 检测中 2成功 3 失败
     */
    private Integer modelDetect;
    /** 规则检测结果 */
    private String ruleDetail;
    /** 模型检测结果 */
    private String modelDetail;
}
