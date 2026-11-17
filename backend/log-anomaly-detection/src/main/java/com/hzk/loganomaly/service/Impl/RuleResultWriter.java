package com.hzk.loganomaly.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hzk.loganomaly.entity.AccessLog;
import com.hzk.loganomaly.entity.Alert;
import com.hzk.loganomaly.rule.IpFeature;
import com.hzk.loganomaly.rule.RuleHit;
import com.hzk.loganomaly.service.AccessLogService;
import com.hzk.loganomaly.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleResultWriter {

    private final AccessLogService accessLogService;
    private final AlertService alertService;

    @Transactional(rollbackFor = Exception.class)
    public void write(Long taskId,
                      Map<String, List<RuleHit>> hitMap,
                      Map<String, IpFeature> featureMap) {
        // 重置规则判断标记
        accessLogService.update(new LambdaUpdateWrapper<AccessLog>()
                                        .eq(AccessLog::getUploadTaskId, taskId)
                                        .set(AccessLog::getRuleAbnormal, 0)
                                        .set(AccessLog::getRuleType, null)
                                        .setSql("final_abnormal = model_abnormal"));
        //  删除旧警告
        alertService.remove(new LambdaQueryWrapper<Alert>()
                                    .eq(Alert::getTaskId, taskId));

        //  规则命中的IP 写入规则检测结果和警告
        List<Alert> alerts = new ArrayList<>();
        for (Map.Entry<String, List<RuleHit>> entry : hitMap.entrySet()) {
            String ip = entry.getKey();
            List<RuleHit> hits = entry.getValue();

            String ruleType = hits.stream()
                                  .map(RuleHit::type)
                                  .collect(Collectors.joining(","));

            // 写入结果
            accessLogService.update(new LambdaUpdateWrapper<AccessLog>()
                                            .eq(AccessLog::getUploadTaskId, taskId)
                                            .eq(AccessLog::getIp, ip)
                                            .set(AccessLog::getRuleAbnormal, 1)
                                            .set(AccessLog::getRuleType, ruleType)
                                            .set(AccessLog::getFinalAbnormal, 1));

            String alertType = ruleType.length() > 50 ? "多规则命中" : ruleType;

            String description = hits.stream()
                                     .map(RuleHit::description)
                                     .collect(Collectors.joining("；"));
            if (description.length() > 500) {
                description = description.substring(0, 497) + "...";
            }

            Alert alert = new Alert();
            alert.setTaskId(taskId);
            alert.setIp(ip);
            alert.setAlertType(alertType);
            alert.setAlertSource("rule");
            alert.setDescription(description);
            alert.setTriggerTime(featureMap.get(ip).getLastAccessTime());
            alert.setStatus(0);
            alerts.add(alert);
        }

        if (!alerts.isEmpty()) {
            alertService.saveBatch(alerts);
        }

        log.info("任务 {} 规则写回完成：命中 IP {} 个，告警 {} 条",
                 taskId, hitMap.size(), alerts.size());
    }
}
