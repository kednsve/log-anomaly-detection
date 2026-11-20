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

    @PostMapping("/runRule")
    public Result runRule(Long taskId) {
        detectService.claimRuleDetect(taskId);
        detectService.runRuleAsync(taskId);
        return Result.success("规则检测已提交，正在后台执行", taskId);
    }

    @PostMapping("/runModel")
    public Result runModel(Long taskId) {
        detectService.claimModelDetect(taskId);
        detectService.runModelAsync(taskId);
        return Result.success("模型检测已提交，正在后台执行", taskId);
    }

    @PostMapping("/runAll")
    public Result runAll(Long taskId) {
        detectService.claimDetectAll(taskId);
        detectService.runAllAsync(taskId);
        return Result.success("检测已提交，正在后台执行", taskId);
    }
}
