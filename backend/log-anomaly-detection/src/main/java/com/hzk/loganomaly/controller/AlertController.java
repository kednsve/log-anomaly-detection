package com.hzk.loganomaly.controller;

import com.hzk.loganomaly.common.Result;
import com.hzk.loganomaly.dto.AlertDTO;
import com.hzk.loganomaly.entity.Alert;
import com.hzk.loganomaly.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/Alert")
public class AlertController {
    private final AlertService alertService;

    @PutMapping("/update")
    public Result AlertUpd(Alert alert) {
        return Result.success(alertService.updateById(alert));
    }

    @GetMapping("/query")
    public Result query(AlertDTO alertDTO) {
        return Result.success(alertService.query(alertDTO));
    }
}
