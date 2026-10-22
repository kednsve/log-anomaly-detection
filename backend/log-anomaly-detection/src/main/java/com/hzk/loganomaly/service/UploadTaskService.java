package com.hzk.loganomaly.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hzk.loganomaly.entity.UploadTask;
import org.springframework.web.multipart.MultipartFile;

public interface UploadTaskService extends IService<UploadTask> {
    void uploadCSV(MultipartFile file);
}
