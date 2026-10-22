package com.hzk.loganomaly.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hzk.loganomaly.entity.AccessLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {
}
