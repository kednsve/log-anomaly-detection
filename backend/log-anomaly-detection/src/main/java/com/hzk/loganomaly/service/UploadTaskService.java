package com.hzk.loganomaly.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hzk.loganomaly.dto.UploadTaskDTO;
import com.hzk.loganomaly.entity.UploadTask;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface UploadTaskService extends IService<UploadTask> {
    UploadTask createTask(String fileName);
    void asyncUploadCSV(Long taskId,File file);
    Page<UploadTask> query(UploadTaskDTO uploadTaskDTO);
}
