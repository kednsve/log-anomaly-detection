package com.hzk.loganomaly.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hzk.loganomaly.dto.AccessLogDTO;
import com.hzk.loganomaly.entity.AccessLog;

import java.util.List;

public interface AccessLogService extends IService<AccessLog> {
    Page<AccessLog> query(AccessLogDTO accessLogDTO);

    void saveLogs(List<AccessLog> logs, Long taskId);
}
