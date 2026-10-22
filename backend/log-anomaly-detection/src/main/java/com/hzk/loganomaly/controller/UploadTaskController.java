package com.hzk.loganomaly.controller;

import com.hzk.loganomaly.common.Result;
import com.hzk.loganomaly.service.UploadTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
        uploadTaskService.uploadCSV(file);
        return Result.success("文件导入成功");
    }
}