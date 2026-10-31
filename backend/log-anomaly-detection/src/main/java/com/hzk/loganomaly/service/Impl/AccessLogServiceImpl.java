package com.hzk.loganomaly.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzk.loganomaly.common.utils.ChkNotNull;
import com.hzk.loganomaly.dto.AccessLogDTO;
import com.hzk.loganomaly.entity.AccessLog;
import com.hzk.loganomaly.mapper.AccessLogMapper;
import com.hzk.loganomaly.service.AccessLogService;
import org.springframework.stereotype.Service;

@Service
public class AccessLogServiceImpl extends ServiceImpl<AccessLogMapper, AccessLog> implements AccessLogService {
    @Override
    public Page<AccessLog> query(AccessLogDTO accessLogDTO) {
        Page<AccessLog> page = new Page<>(accessLogDTO.getPageNum(), accessLogDTO.getPageSize());
        return this.page(
                page, new LambdaQueryWrapper<AccessLog>()
                        .like(ChkNotNull.check(accessLogDTO.getIp()), AccessLog::getIp, accessLogDTO.getIp())
                        .eq(
                                ChkNotNull.check(accessLogDTO.getStatusCode()),
                                AccessLog::getStatusCode,
                                accessLogDTO.getStatusCode()
                        )
                        .eq(ChkNotNull.check(accessLogDTO.getMethod()), AccessLog::getMethod, accessLogDTO.getMethod())
                        .like(
                                ChkNotNull.check(accessLogDTO.getIpLocation()),
                                AccessLog::getIpLocation,
                                accessLogDTO.getIpLocation()
                        )
                        .eq(
                                ChkNotNull.check(accessLogDTO.getFinalAbnormal()),
                                AccessLog::getFinalAbnormal,
                                accessLogDTO.getFinalAbnormal()
                        )
                        .ge(
                                ChkNotNull.check(accessLogDTO.getStartTime()),
                                AccessLog::getAccessTime,
                                accessLogDTO.getStartTime()
                        )
                        .le(
                                ChkNotNull.check(accessLogDTO.getEndTime()),
                                AccessLog::getAccessTime,
                                accessLogDTO.getEndTime()
                        )
        );
    }

}
