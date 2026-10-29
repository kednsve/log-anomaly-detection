package com.hzk.loganomaly.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzk.loganomaly.common.exception.BusinessException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadTaskServiceImpl extends ServiceImpl<UploadTaskMapper, UploadTask> implements UploadTaskService {
    private final AccessLogService accessLogService;

    /**
     * 上传文件
     *
     * @param file 上传的csv文件, spring 的 MultipartFile
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Async
    public void asyncUploadCSV(File file, String fileName) {
        UploadTask uploadTask = new UploadTask();
        uploadTask.setFileName(fileName);
        uploadTask.setStatus(0);
        this.save(uploadTask);

        try (InputStream is = new FileInputStream(file)) {
            CsvUtil.ParseResult parseResult = CsvUtil.parse(is);
            List<AccessLog> validLogs = parseResult.getValidLogs();

            if (!validLogs.isEmpty()) {
                validLogs.forEach(log -> log.setUploadTaskId(uploadTask.getId()));
                accessLogService.saveBatch(validLogs, 2000);
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
            this.updateById(uploadTask);

            log.info("文件 {} 解析完成，成功入库 {} 条", fileName, validLogs.size());

        } catch (Exception e) {
            log.error("异步导入文件失败", e);
            uploadTask.setStatus(2);
            uploadTask.setFailDetail(e.getMessage());
            this.updateById(uploadTask);
        } finally {
            if (file.exists() && file.delete()) {
                log.info("临时文件已清理：{}", file.getName());
            }
        }
    }
}
