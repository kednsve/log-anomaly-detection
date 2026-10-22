package com.hzk.loganomaly.service;

import com.hzk.loganomaly.dto.LoginDTO;
import com.hzk.loganomaly.vo.LoginVO;

public interface AuthService {
    /**
     * 登录
     * @param loginDTO 包含账号密码
     * @return 返回Token和用户信息
     */
    LoginVO login(LoginDTO loginDTO);
}
