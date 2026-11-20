package com.hzk.loganomaly.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hzk.loganomaly.common.exception.BusinessException;
import com.hzk.loganomaly.config.ModelDetectConfig;
import com.hzk.loganomaly.dto.ModelDetectResponse;
import com.hzk.loganomaly.entity.UploadTask;
import com.hzk.loganomaly.mapper.AccessLogMapper;
import com.hzk.loganomaly.model.ModelDetectClient;
import com.hzk.loganomaly.rule.IpFeature;
import com.hzk.loganomaly.rule.RuleEngine;
import com.hzk.loganomaly.rule.RuleHit;
import com.hzk.loganomaly.service.DetectService;
import com.hzk.loganomaly.service.UploadTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class DetectServiceImpl implements DetectService {

    private final UploadTaskService uploadTaskService;
    private final AccessLogMapper accessLogMapper;
    private final RuleEngine ruleEngine;
    private final RuleResultWriter ruleResultWriter;

    private final ModelDetectClient modelDetectClient;
    private final ModelDetectConfig modelDetectConfig;
    private final ModelResultWriter modelResultWriter;

    /**
     * 校验任务
     */
    private void validateTask(Long taskId) {
        UploadTask uploadTask = uploadTaskService.getById(taskId);
        if (uploadTask == null) {
            throw new BusinessException(404, "上传任务不存在");
        }
        if (uploadTask.getStatus() == null || uploadTask.getStatus() != 1) {
            throw new BusinessException(409, "上传任务尚未完成");
        }
        if (uploadTask.getSuccessRows() == null || uploadTask.getSuccessRows() == 0) {
            throw new BusinessException(404, "没有可检测的数据");
        }
    }

    /**
     * 抢占规则层
     */
    private boolean claimRule(Long taskId) {
        return uploadTaskService.update(
                new LambdaUpdateWrapper<UploadTask>()
                        .eq(UploadTask::getId, taskId)
                        .eq(UploadTask::getStatus, 1)
                        .in(UploadTask::getRuleDetect, 0, 2, 3)
                        .set(UploadTask::getRuleDetect, 1)
                        .set(UploadTask::getRuleDetail, null));
    }

    /**
     * 抢占模型层
     */
    private boolean claimModel(Long taskId) {
        return uploadTaskService.update(
                new LambdaUpdateWrapper<UploadTask>()
                        .eq(UploadTask::getId, taskId)
                        .eq(UploadTask::getStatus, 1)
                        .in(UploadTask::getModelDetect, 0, 2, 3)
                        .set(UploadTask::getModelDetect, 1)
                        .set(UploadTask::getModelDetail, null));
    }

    /**
     * 抢占两层
     */
    private boolean claimBoth(Long taskId) {
        return uploadTaskService.update(
                new LambdaUpdateWrapper<UploadTask>()
                        .eq(UploadTask::getId, taskId)
                        .eq(UploadTask::getStatus, 1)
                        .in(UploadTask::getRuleDetect, 0, 2, 3)
                        .in(UploadTask::getModelDetect, 0, 2, 3)
                        .set(UploadTask::getRuleDetect, 1)
                        .set(UploadTask::getModelDetect, 1)
                        .set(UploadTask::getRuleDetail, null)
                        .set(UploadTask::getModelDetail, null));
    }

    /**
     * 修改检测状态
     */
    private void markState(Long taskId, boolean isRule, int state, String detail) {
        LambdaUpdateWrapper<UploadTask> wrapper = new LambdaUpdateWrapper<UploadTask>()
                .eq(UploadTask::getId, taskId);
        if (isRule) {
            wrapper.set(UploadTask::getRuleDetect, state)
                   .set(UploadTask::getRuleDetail, detail);
        } else {
            wrapper.set(UploadTask::getModelDetect, state)
                   .set(UploadTask::getModelDetail, detail);
        }
        uploadTaskService.update(wrapper);
    }

    /**
     * 统计每条规则命中的 IP 数
     */
    private Map<String, Integer> countByRule(Map<String, List<RuleHit>> hitMap) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        hitMap.values().forEach(
                hits ->
                        hits.forEach(hit -> counts.merge(hit.type(), 1, Integer::sum)));
        return counts;
    }

    // 规则检测
    @Override
    public void claimRuleDetect(Long taskId) {
        validateTask(taskId);
        if (!claimRule(taskId)) {
            throw new BusinessException(409, "该任务正在检测中");
        }
    }

    /**
     * 规则检测执行
     * 按 IP 聚合 → 规则匹配 → 事务写回 → rule_detect 置 2/3
     */
    private void executeRule(Long taskId) {
        long startTime = System.currentTimeMillis();
        try {
            List<IpFeature> ipFeatures = accessLogMapper.selectIpFeatures(taskId);
            Map<String, List<RuleHit>> hitMap = new LinkedHashMap<>();
            Map<String, IpFeature> featureMap = new HashMap<>();
            for (IpFeature ipFeature : ipFeatures) {
                List<RuleHit> hits = ruleEngine.evaluate(ipFeature);
                if (!hits.isEmpty()) {
                    hitMap.put(ipFeature.getIp(), hits);
                    featureMap.put(ipFeature.getIp(), ipFeature);
                }
            }
            ruleResultWriter.write(taskId, hitMap, featureMap);

            long elapsedMs = System.currentTimeMillis() - startTime;
            String detail = JSON.toJSONString(Map.of(
                    "ipCount", ipFeatures.size(),
                    "abnormalIpCount", hitMap.size(),
                    "elapsedMs", elapsedMs,
                    "finishedAt", LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));
            markState(taskId, true, 2, detail);
            log.info(
                    "任务 {} 规则检测完成：IP {} 个，异常 IP {} 个，耗时 {} ms",
                    taskId, ipFeatures.size(), hitMap.size(), elapsedMs
            );
        } catch (RuntimeException e) {
            try {
                markState(taskId, true, 3, null);
            } catch (Exception markException) {
                log.error(
                        "更新任务 {} 的规则检测状态失败：{}",
                        taskId, markException.getMessage(), markException
                );
            }
            log.error("任务 {} 规则检测失败：{}", taskId, e.getMessage(), e);
            throw e;
        }
    }

    // 模型检测
    @Override
    public void claimModelDetect(Long taskId) {
        validateTask(taskId);
        if (!modelDetectConfig.isEnabled()) {
            throw new BusinessException(409, "模型检测未启用");
        }
        if (!claimModel(taskId)) {
            throw new BusinessException(409, "该任务的模型检测正在执行中");
        }
    }

    /**
     * 模型检测执行：假定已抢占 model_detect=1。
     * 调 Python 推理 → 事务写回 → model_detect 置 2/3。
     */
    private void executeModel(Long taskId) {
        long startTime = System.currentTimeMillis();
        try {
            ModelDetectResponse response = modelDetectClient.detect(taskId);
            int abnormalCount = modelResultWriter.write(taskId, response);
            long elapsedMs = System.currentTimeMillis() - startTime;
            String detail = JSON.toJSONString(Map.of(
                    "ipCount", response.getIpCount(),
                    "abnormalIpCount", abnormalCount,
                    "elapsedMs", elapsedMs,
                    "finishedAt", LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            ));
            markState(taskId, false, 2, detail);

            log.info(
                    "任务 {} 模型检测完成：IP {} 个，异常 IP {} 个，耗时 {} ms",
                    taskId, response.getIpCount(), abnormalCount, elapsedMs
            );
        } catch (RuntimeException e) {
            try {
                markState(taskId, false, 3, null);
            } catch (Exception markException) {
                log.error(
                        "更新任务 {} 的模型检测状态失败：{}",
                        taskId, markException.getMessage(), markException
                );
            }
            log.error("任务 {} 模型检测失败：{}", taskId, e.getMessage(), e);
            throw new BusinessException(500, "模型检测失败：" + e.getMessage());
        }
    }

    // 两层
    @Override
    public void claimDetectAll(Long taskId) {
        validateTask(taskId);
        if (!modelDetectConfig.isEnabled()) {
            throw new BusinessException(409, "模型检测未启用");
        }
        if (!claimBoth(taskId)) {
            throw new BusinessException(409, "该任务正在检测中");
        }
    }

    @Async
    @Override
    public void runRuleAsync(Long taskId) {
        try {
            executeRule(taskId);
        } catch (RuntimeException ignored) {
        }
    }

    @Async
    @Override
    public void runModelAsync(Long taskId) {
        try {
            executeModel(taskId);
        } catch (RuntimeException ignored) {
        }
    }

    /**
     * 两层串行：先规则后模型；规则失败不阻断模型，两层状态各自独立置 2/3
     */
    @Async
    @Override
    public void runAllAsync(Long taskId) {
        try {
            executeRule(taskId);
        } catch (RuntimeException ignored) {
        }
        try {
            executeModel(taskId);
        } catch (RuntimeException ignored) {
        }
    }
}
