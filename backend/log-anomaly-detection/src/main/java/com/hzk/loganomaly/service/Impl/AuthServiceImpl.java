package com.hzk.loganomaly.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hzk.loganomaly.common.exception.BusinessException;
import com.hzk.loganomaly.common.utils.ChkNotNull;
import com.hzk.loganomaly.common.utils.JwtUtil;
import com.hzk.loganomaly.dto.LoginDTO;
import com.hzk.loganomaly.entity.User;
import com.hzk.loganomaly.mapper.UserMapper;
import com.hzk.loganomaly.service.AuthService;
import com.hzk.loganomaly.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        if (!ChkNotNull.check(loginDTO.getUsername()) || !ChkNotNull.check(loginDTO.getPassword())) {
            throw new BusinessException("用户名或密码不能为空");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, loginDTO.getUsername()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setRole(user.getRole());
        loginVO.setNickname(user.getNickname());

        return loginVO;
    }
}
