package com.hzk.loganomaly.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzk.loganomaly.common.utils.CsvUtil;
import com.hzk.loganomaly.entity.AccessLog;
import com.hzk.loganomaly.entity.UploadTask;
import com.hzk.loganomaly.mapper.UploadTaskMapper;
import com.hzk.loganomaly.service.AccessLogService;
import com.hzk.loganomaly.service.UploadTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadTaskServiceImpl extends ServiceImpl<UploadTaskMapper, UploadTask> implements UploadTaskService {
    private final AccessLogService accessLogService;

    @Override
    public UploadTask createTask(String fileName) {
        UploadTask uploadTask = new UploadTask();
        uploadTask.setFileName(fileName);
        uploadTask.setStatus(0);
        this.save(uploadTask);
        return uploadTask;
    }

    /**
     *
     * @param taskId 任务id
     * @param file   上传的文件
     */
    @Override
    @Async
    public void asyncUploadCSV(Long taskId, File file) {
        UploadTask uploadTask = null;
        try {
            uploadTask = this.getById(taskId);
            if (uploadTask == null) {
                log.error("上传任务不存在：{}", taskId);
                return;
            }
            CsvUtil.ParseResult parseResult;
            try (InputStream is = new FileInputStream(file)) {
                parseResult = CsvUtil.parse(is);
            }
            List<AccessLog> validLogs = parseResult.getValidLogs();

            if (!validLogs.isEmpty()) {
                accessLogService.saveLogs(validLogs, uploadTask.getId());
            }

            uploadTask.setStatus(1);
            uploadTask.setTotalRows(parseResult.getTotalRows());
            uploadTask.setSuccessRows(validLogs.size());
            uploadTask.setFailRows(parseResult.getErrors().size());

            List<String> errors = parseResult.getErrors();
            String failDetailJson = JSON.toJSONString(errors);
            if (failDetailJson.length() > 5000) {
                failDetailJson = failDetailJson.substring(0, 5000) + "...(更多错误已省略)";
            }
            uploadTask.setFailDetail(failDetailJson);
            uploadTask.setFinishTime(LocalDateTime.now());
            this.updateById(uploadTask);

            log.info("文件 {} 解析完成，成功入库 {} 条", uploadTask.getFileName(), validLogs.size());

        } catch (Exception e) {
            log.error("异步导入文件失败：{}", e.getMessage(), e);
            if (uploadTask != null) {
                uploadTask.setStatus(2);
                uploadTask.setFailDetail(e.getMessage());
                uploadTask.setFinishTime(LocalDateTime.now());
                this.updateById(uploadTask);
            }
        } finally {
            if (file.exists() && file.delete()) {
                log.info("临时文件已清理：{}", file.getName());
            }
        }
    }
}
