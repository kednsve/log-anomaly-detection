package com.hzk.loganomaly.controller;

import com.hzk.loganomaly.common.Result;
import com.hzk.loganomaly.service.UploadTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadTaskController {
    private final UploadTaskService uploadTaskService;
    @PostMapping("/csv")
    public Result uploadCSV(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件为空");
        }
        if(!file.getOriginalFilename().endsWith(".csv")){
            return Result.error("仅支持csv文件");
        }
        try{
            String tempDirPath = System.getProperty("java.io.tmpdir") + File.separator + "log_anomaly_temp";
            File dir = new File(tempDirPath);
            if(!dir.exists())
                dir.mkdirs();
            String fileName= System.currentTimeMillis()+"_"+file.getOriginalFilename();
            File localFile = new File(dir,fileName);
            file.transferTo(localFile);
            uploadTaskService.asyncUploadCSV(localFile,file.getOriginalFilename());
            return Result.success("文件导入成功");
        } catch (IOException e) {
            log.error("文件导入失败：{}",e.getMessage());
            return Result.error("文件保存失败");
        }
    }
}
