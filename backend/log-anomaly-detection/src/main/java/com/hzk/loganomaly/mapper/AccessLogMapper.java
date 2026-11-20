package com.hzk.loganomaly.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hzk.loganomaly.entity.AccessLog;
import com.hzk.loganomaly.rule.IpFeature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {
    List<IpFeature> selectIpFeatures(Long taskId);
}
