package com.hzk.loganomaly.controller;

import com.hzk.loganomaly.common.Result;
import com.hzk.loganomaly.dto.AccessLogDTO;
import com.hzk.loganomaly.entity.AccessLog;
import com.hzk.loganomaly.service.AccessLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("AccessLog")
public class AccessLogController {
    private final AccessLogService accessLogService;

    @GetMapping("/query")
    public Result AccessLogQuery(AccessLogDTO accessLogDTO) {
        return Result.success(accessLogService.query(accessLogDTO));
    }

    @PutMapping("/update")
    public Result AccessLogUpd(AccessLog accessLog) {
        return Result.success(accessLogService.updateById(accessLog));
    }
}
