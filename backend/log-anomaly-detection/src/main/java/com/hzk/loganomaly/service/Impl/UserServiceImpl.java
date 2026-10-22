package com.hzk.loganomaly.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hzk.loganomaly.entity.User;
import com.hzk.loganomaly.mapper.UserMapper;
import com.hzk.loganomaly.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
