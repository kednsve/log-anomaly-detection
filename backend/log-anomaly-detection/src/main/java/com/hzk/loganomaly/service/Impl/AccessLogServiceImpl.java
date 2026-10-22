package com.hzk.loganomaly.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzk.loganomaly.entity.AccessLog;
import com.hzk.loganomaly.mapper.AccessLogMapper;
import com.hzk.loganomaly.service.AccessLogService;
import org.springframework.stereotype.Service;

@Service
public class AccessLogServiceImpl extends ServiceImpl<AccessLogMapper, AccessLog> implements AccessLogService {
}
