package com.hzk.loganomaly.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 上传任务查询条件
 */
@Data
public class UploadTaskDTO {

    /**
     * 当前页码（默认第 1 页）
     */
    private Long pageNum = 1L;

    /**
     * 每页条数（默认 10 条）
     */
    private Long pageSize = 10L;

    /**
     * 任务 ID
     */
    private Long id;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 任务状态：0-处理中，1-完成，2-失败
     */
    private Integer status;
    /**
     * 规则检测状态 0 未检测 1 检测中 2成功 3 失败
     */
    private Integer ruleDetect;
    /**
     * 模型检测状态 0 未检测 1 检测中 2成功 3 失败
     */
    private Integer modelDetect;
    /**
     * 上传时间范围-开始
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startUploadTime;

    /**
     * 上传时间范围-结束
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endUploadTime;

    /**
     * 完成时间范围-开始
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startFinishTime;

    /**
     * 完成时间范围-结束
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endFinishTime;
}
