package com.hzk.loganomaly.service.Impl;

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
        hitMap.values().forEach(hits ->
                                        hits.forEach(hit -> counts.merge(hit.type(), 1, Integer::sum)));
        return counts;
    }

    @Override
    public DetectResultDTO detect(Long taskId) {
        long startTime = System.currentTimeMillis();
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

        ruleResultWriter.write(taskId, hitMap, featureMap);

        DetectResultDTO detectResultDTO = new DetectResultDTO();
        detectResultDTO.setTaskId(taskId);
        detectResultDTO.setIpCount(ipFeatures.size());
        detectResultDTO.setAbnormalIpCount(hitMap.size());
        detectResultDTO.setAlertCount(hitMap.size());
        detectResultDTO.setRuleHitCounts(countByRule(hitMap));
        detectResultDTO.setElapsedMs(System.currentTimeMillis() - startTime);

        log.info(
                "任务 {} 检测完成：IP {} 个，异常 IP {} 个，耗时 {} ms",
                taskId,
                ipFeatures.size(),
                hitMap.size(),
                detectResultDTO.getElapsedMs()
        );

        return detectResultDTO;
    }


}
