package com.hzk.loganomaly.service.Impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hzk.loganomaly.entity.UploadTask;
import com.hzk.loganomaly.service.UploadTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动时清理残留的"检测中"状态。
 * 进程崩溃/强杀时，rule_detect / model_detect 会停留在 1，
 * 导致该任务永远无法再次提交检测，这里统一置为 3（失败）并写入原因。
 * 注意：仅适用于单实例部署。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DetectStateCleaner {

    private final UploadTaskService uploadTaskService;

    @EventListener(ApplicationReadyEvent.class)
    public void resetStaleDetectStates() {
        boolean ruleCount = uploadTaskService.update(
                new LambdaUpdateWrapper<UploadTask>()
                        .eq(UploadTask::getRuleDetect, 1)
                        .set(UploadTask::getRuleDetect, 3));
        boolean modelCount = uploadTaskService.update(
                new LambdaUpdateWrapper<UploadTask>()
                        .eq(UploadTask::getModelDetect, 1)
                        .set(UploadTask::getModelDetect, 3));
        if (ruleCount || modelCount) {
            log.warn("启动清理：残留检测中状态已置为失败");
        }
    }
}