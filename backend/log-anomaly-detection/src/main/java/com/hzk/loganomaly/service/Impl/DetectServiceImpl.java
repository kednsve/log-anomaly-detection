package com.hzk.loganomaly.service.Impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hzk.loganomaly.common.exception.BusinessException;
import com.hzk.loganomaly.dto.DetectResultDTO;
import com.hzk.loganomaly.entity.UploadTask;
import com.hzk.loganomaly.mapper.AccessLogMapper;
import com.hzk.loganomaly.rule.IpFeature;
import com.hzk.loganomaly.rule.RuleEngine;
import com.hzk.loganomaly.rule.RuleHit;
import com.hzk.loganomaly.service.DetectService;
import com.hzk.loganomaly.service.UploadTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    /**
     * 规则检测
     * 对指定上传任务执行规则检测。
     * 流程：校验任务 → 抢占 rule_detect → 按 IP 聚合 → 规则匹配 → 事务写回 → 更新检测状态。
     * 同一任务并发调用时只有一个请求能执行，其余返回 409。
     * @param taskId uploadTaskId
     * @return  检测结果
     */
    @Override
    public DetectResultDTO detect(Long taskId) {
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
        // 抢占 把 rule_detect 置 1
        // rule_detect 0(未检测)/1(检测中)/2(完成)/3(失败)
        boolean update = uploadTaskService.update(
                new LambdaUpdateWrapper<UploadTask>()
                        .eq(UploadTask::getId, taskId)
                        .eq(UploadTask::getStatus, 1)
                        .in(UploadTask::getRuleDetect, 0, 2, 3)
                        .set(UploadTask::getRuleDetect, 1)
        );
        if (!update) {
            throw new BusinessException(409, "该任务正在检测中");
        }

        long startTime = System.currentTimeMillis();
        try {
            List<IpFeature> ipFeatures = accessLogMapper.selectIpFeatures(taskId);
            // 规则匹配
            Map<String, List<RuleHit>> hitMap = new LinkedHashMap<>();
            Map<String, IpFeature> featureMap = new HashMap<>();
            for (IpFeature ipFeature : ipFeatures) {
                List<RuleHit> hits = ruleEngine.evaluate(ipFeature);
                if (!hits.isEmpty()) {
                    hitMap.put(ipFeature.getIp(), hits);
                    featureMap.put(ipFeature.getIp(), ipFeature);
                }
            }
            // 写回
            ruleResultWriter.write(taskId, hitMap, featureMap);

            DetectResultDTO detectResultDTO = new DetectResultDTO();
            detectResultDTO.setTaskId(taskId);
            detectResultDTO.setIpCount(ipFeatures.size());
            detectResultDTO.setAbnormalIpCount(hitMap.size());
            detectResultDTO.setAlertCount(hitMap.size());
            detectResultDTO.setRuleHitCounts(countByRule(hitMap));
            detectResultDTO.setElapsedMs(System.currentTimeMillis() - startTime);
            // 成功：把 rule_detect 置 2
            // rule_detect 0(未检测)/1(检测中)/2(完成)/3(失败)
            uploadTaskService.update(
                    new LambdaUpdateWrapper<UploadTask>()
                            .eq(UploadTask::getId, taskId)
                            .set(UploadTask::getRuleDetect, 2)
            );
            log.info(
                    "任务 {} 检测完成：IP {} 个，异常 IP {} 个，耗时 {} ms",
                    taskId,
                    ipFeatures.size(),
                    hitMap.size(),
                    detectResultDTO.getElapsedMs()
            );
            return detectResultDTO;
        } catch (RuntimeException e) {
            try {
                // 失败：把 rule_detect 置 3
                // rule_detect 0(未检测)/1(检测中)/2(完成)/3(失败)
                uploadTaskService.update(
                        new LambdaUpdateWrapper<UploadTask>()
                                .eq(UploadTask::getId, taskId)
                                .set(UploadTask::getRuleDetect, 3)
                );
            } catch (Exception markException) {
                log.error("更新任务 {} 的规则检测状态失败：{}", taskId, markException.getMessage(), markException);
            }
            log.error("任务 {} 规则检测失败：{}", taskId, e.getMessage(), e);
            throw e;
        }
    }

}
