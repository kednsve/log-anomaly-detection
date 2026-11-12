package com.hzk.loganomaly.controller;

import com.hzk.loganomaly.common.Result;
import com.hzk.loganomaly.entity.UploadTask;
import com.hzk.loganomaly.service.UploadTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadTaskController {
    private final UploadTaskService uploadTaskService;

    @PostMapping("/csv")
    public Result uploadCSV(@RequestParam("file") MultipartFile file) {
        if (file ==null || file.isEmpty()) {
            return Result.error("上传文件为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            return Result.error("仅支持csv文件");
        }
        String tempDirPath = System.getProperty("java.io.tmpdir") + File.separator + "log_anomaly_temp";
        File dir = new File(tempDirPath);
        if(!dir.exists())
            dir.mkdirs();
        String fileName= UUID.randomUUID()+"_"+originalFilename;
        File localFile = new File(dir,fileName);
        UploadTask task = null;
        try{
            task = uploadTaskService.createTask(originalFilename);
            file.transferTo(localFile);
            uploadTaskService.asyncUploadCSV(task.getId(), localFile);
            return Result.success("文件已接收，正在后台处理",task.getId());
        } catch (Exception e) {
            log.error("文件处理失败：{}", e.getMessage(), e);
            if (localFile.exists()) {
                localFile.delete();
            }
            if (task != null) {
                task.setStatus(2);
                task.setFailDetail("任务提交失败");
                task.setFinishTime(LocalDateTime.now());
                uploadTaskService.updateById(task);
            }
            return Result.error("文件处理失败");
        }
    }
}
