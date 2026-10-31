package com.hzk.loganomaly.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzk.loganomaly.common.utils.ChkNotNull;
import com.hzk.loganomaly.dto.AlertDTO;
import com.hzk.loganomaly.entity.Alert;
import com.hzk.loganomaly.mapper.AlertMapper;
import com.hzk.loganomaly.service.AlertService;
import org.springframework.stereotype.Service;

@Service
public class AlertServiceImpl extends ServiceImpl<AlertMapper, Alert> implements AlertService {
    @Override
    public Page<Alert> query(AlertDTO alertDTO) {
        Page<Alert> page = new Page<>(alertDTO.getPageNum(), alertDTO.getPageSize());
        return this.page(
                page,
                new LambdaQueryWrapper<Alert>()
                        .eq(ChkNotNull.check(alertDTO.getId()), Alert::getId, alertDTO.getId())
                        .eq(
                                ChkNotNull.check(alertDTO.getTaskId()),
                                Alert::getTaskId,
                                alertDTO.getTaskId()
                        )
                        .like(
                                ChkNotNull.check(alertDTO.getIp()),
                                Alert::getIp,
                                alertDTO.getIp()
                        )
                        .eq(ChkNotNull.check(alertDTO.getAlertType()), Alert::getAlertType, alertDTO.getAlertType())
                        .eq(
                                ChkNotNull.check(alertDTO.getAlertSource()),
                                Alert::getAlertSource,
                                alertDTO.getAlertSource()
                        )
                        .like(
                                ChkNotNull.check(alertDTO.getDescription()),
                                Alert::getDescription,
                                alertDTO.getDescription()
                        )
                        .ge(
                                ChkNotNull.check(alertDTO.getStartTriggerTime()),
                                Alert::getTriggerTime,
                                alertDTO.getStartTriggerTime()
                        )
                        .le(
                                ChkNotNull.check(alertDTO.getEndTriggerTime()),
                                Alert::getTriggerTime,
                                alertDTO.getEndTriggerTime()
                        )
                        .eq(ChkNotNull.check(alertDTO.getStatus()), Alert::getStatus, alertDTO.getStatus())
                        .ge(
                                ChkNotNull.check(alertDTO.getStartHandleTime()),
                                Alert::getHandleTime,
                                alertDTO.getStartHandleTime()
                        )
                        .le(
                                ChkNotNull.check(alertDTO.getEndHandleTime()),
                                Alert::getHandleTime,
                                alertDTO.getEndHandleTime()
                        )
        );
    }
}
