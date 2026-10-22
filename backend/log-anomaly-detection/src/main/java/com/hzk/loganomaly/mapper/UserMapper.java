package com.hzk.loganomaly.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hzk.loganomaly.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
