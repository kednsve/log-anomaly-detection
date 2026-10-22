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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    public void uploadCSV(MultipartFile file) {
        UploadTask uploadTask = new UploadTask();
        uploadTask.setFileName(file.getOriginalFilename());
        uploadTask.setStatus(0);
        this.save(uploadTask);

        try {
            CsvUtil.ParseResult parseResult = CsvUtil.parse(file.getInputStream());
            List<AccessLog> validLogs = parseResult.getValidLogs();

            if (!validLogs.isEmpty()) {
                validLogs.forEach(accessLog -> accessLog.setUploadTaskId(uploadTask.getId()));
                accessLogService.saveBatch(validLogs,500);
            }

            uploadTask.setStatus(1);
            uploadTask.setTotalRows(parseResult.getTotalRows());
            uploadTask.setSuccessRows(validLogs.size());
            uploadTask.setFailRows(parseResult.getErrors().size());
            uploadTask.setFailDetail(JSON.toJSONString(parseResult.getErrors()));

        } catch (IOException e) {
            log.warn("csv导入失败：{}",e.getMessage());
            uploadTask.setStatus(2);
            uploadTask.setFailDetail(e.getMessage());
            throw new BusinessException("文件解析失败");
        }
    }
}
