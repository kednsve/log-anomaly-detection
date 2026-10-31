package com.hzk.loganomaly.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hzk.loganomaly.dto.AlertDTO;
import com.hzk.loganomaly.entity.Alert;

public interface AlertService extends IService<Alert> {
    Page<Alert> query(AlertDTO alertDTO);
}
