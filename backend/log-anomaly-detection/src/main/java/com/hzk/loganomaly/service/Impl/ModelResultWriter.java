package com.hzk.loganomaly.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hzk.loganomaly.dto.ModelDetectResponse;
import com.hzk.loganomaly.entity.AccessLog;
import com.hzk.loganomaly.entity.Alert;
import com.hzk.loganomaly.service.AccessLogService;
import com.hzk.loganomaly.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelResultWriter {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AccessLogService accessLogService;
    private final AlertService alertService;

    /**
     * 模型检测结果写回：
     * 1. 重置模型标记，final 回退到规则层结果
     * 2. 告警合并：both 退回 rule（内容本来属于规则层），删除模型层告警
     * 3. 模型异常 IP 逐条标记 + 生成/升级告警
     *
     * @return 异常 IP 数
     */
    @Transactional(rollbackFor = Exception.class)
    public int write(Long taskId, ModelDetectResponse response) {
        // 1. 重置模型标记
        accessLogService.update(
                new LambdaUpdateWrapper<AccessLog>()
                        .eq(AccessLog::getUploadTaskId, taskId)
                        .set(AccessLog::getModelScore, 0.0)
                        .set(AccessLog::getModelAbnormal, 0)
                        .setSql("final_abnormal = rule_abnormal"));

        // 2. 告警合并：both → rule（保留规则层内容），再删模型层告警
        alertService.update(new LambdaUpdateWrapper<Alert>()
                                    .eq(Alert::getTaskId, taskId)
                                    .eq(Alert::getAlertSource, "both")
                                    .set(Alert::getAlertSource, "rule"));
        alertService.remove(new LambdaQueryWrapper<Alert>()
                                    .eq(Alert::getTaskId, taskId)
                                    .eq(Alert::getAlertSource, "model"));

        // 3. 规则层告警，按 IP 建索引
        Map<String, Alert> ruleAlerts = alertService
                .list(new LambdaQueryWrapper<Alert>().eq(Alert::getTaskId, taskId))
                .stream()
                .collect(Collectors.toMap(Alert::getIp, Function.identity()));
        // 写回
        int abnormalCount = 0;
        List<Alert> inserts = new ArrayList<>();
        if (response.getScores() != null) {
            for (ModelDetectResponse.IpScore score : response.getScores()) {
                if (!Boolean.TRUE.equals(score.getIsAbnormal())) {
                    continue;// 正常IP跳过
                }
                abnormalCount++;

                // model_score 修改，model_abnormal final 置为1
                accessLogService.update(
                        new LambdaUpdateWrapper<AccessLog>()
                                .eq(AccessLog::getUploadTaskId, taskId)
                                .eq(AccessLog::getIp, score.getIp())
                                .set(AccessLog::getModelScore, score.getScore())
                                .set(AccessLog::getModelAbnormal, 1)
                                .set(AccessLog::getFinalAbnormal, 1));

                Alert existing = ruleAlerts.get(score.getIp());
                if (existing != null) {
                    // 规则已告警，来源置为 both
                    existing.setAlertSource("both");
                    alertService.updateById(existing);
                } else {
                    Alert alert = new Alert();
                    alert.setTaskId(taskId);
                    alert.setIp(score.getIp());
                    alert.setAlertType("模型检测异常");
                    alert.setAlertSource("model");
                    alert.setDescription(String.format("Isolation Forest 判定为异常，异常分数 %.2f",score.getScore()));
                    alert.setTriggerTime(parseTime(score.getLastAccessTime()));
                    alert.setStatus(0);
                    inserts.add(alert);
                }
            }
        }
        if (!inserts.isEmpty()) {
            alertService.saveBatch(inserts);
        }

        log.info("任务 {} 模型写回完成：模型异常 {} 个，新增告警 {} 条",
                 taskId, abnormalCount, inserts.size());
        return abnormalCount;
    }

    private LocalDateTime parseTime(String text) {
        if (text == null || text.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(text, TIME_FORMAT);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}