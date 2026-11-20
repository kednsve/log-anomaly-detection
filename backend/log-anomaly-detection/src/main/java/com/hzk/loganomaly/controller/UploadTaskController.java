package com.hzk.loganomaly.controller;

import com.hzk.loganomaly.common.Result;
import com.hzk.loganomaly.dto.UploadTaskDTO;
import com.hzk.loganomaly.entity.UploadTask;
import com.hzk.loganomaly.service.UploadTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/uploadTask")
@RequiredArgsConstructor
public class UploadTaskController {
    private final UploadTaskService uploadTaskService;

    @PostMapping("/csv")
    public Result uploadCSV(@RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            return Result.error("仅支持csv文件");
        }
        String tempDirPath = System.getProperty("java.io.tmpdir") + File.separator + "log_anomaly_temp";
        File dir = new File(tempDirPath);
        if (!dir.exists())
            dir.mkdirs();
        String fileName = UUID.randomUUID() + "_" + originalFilename;
        File localFile = new File(dir, fileName);
        UploadTask task = null;
        try {
            task = uploadTaskService.createTask(originalFilename);
            file.transferTo(localFile);
            uploadTaskService.asyncUploadCSV(task.getId(), localFile);
            return Result.success("文件已接收，正在后台处理", task.getId());
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

    /**
     * 前端轮询接口
     * @param taskId 任务ID
     * @return 任务对象
     */
    @GetMapping("/polling")
    public Result polling(@RequestParam(required = false) Long taskId){
        if (taskId == null) {
            return Result.error("查询id为空");
        }
        UploadTask uploadTask = uploadTaskService.getById(taskId);
        if (uploadTask == null) {
            return Result.error("上传任务不存在");
        }
        return Result.success(uploadTask);
    }

    @GetMapping("/query")
    public Result uploadTaskQuery(UploadTaskDTO uploadTaskDTO) {
        return Result.success(uploadTaskService.query(uploadTaskDTO));
    }

}
