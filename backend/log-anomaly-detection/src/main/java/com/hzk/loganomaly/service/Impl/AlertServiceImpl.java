package com.hzk.loganomaly.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzk.loganomaly.entity.Alert;
import com.hzk.loganomaly.mapper.AlertMapper;
import com.hzk.loganomaly.service.AlertService;
import org.springframework.stereotype.Service;

@Service
public class AlertServiceImpl extends ServiceImpl<AlertMapper, Alert> implements AlertService {
}
