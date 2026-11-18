package com.hzk.loganomaly.controller;

import com.hzk.loganomaly.common.Result;
import com.hzk.loganomaly.service.DetectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Detect")
@RequiredArgsConstructor
public class DetectController {
    private final DetectService detectService;

    @PostMapping("/run")
    public Result run(Long taskId){
        return Result.success(detectService.detect(taskId));
    }
}
