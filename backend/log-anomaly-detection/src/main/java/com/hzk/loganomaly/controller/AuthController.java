package com.hzk.loganomaly.controller;

import com.hzk.loganomaly.common.Result;
import com.hzk.loganomaly.dto.LoginDTO;
import com.hzk.loganomaly.entity.User;
import com.hzk.loganomaly.service.AuthService;
import com.hzk.loganomaly.service.UserService;
import com.hzk.loganomaly.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    /**
     * 登录
     * @param loginDTO 包含username password
     * @return result{code,msg,data}
     */
    @PostMapping
    public Result login(@RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = authService.login(loginDTO);
        return Result.success(loginVO);
    }
    /**
     * reset password
     */
    @PutMapping
    public Result reset(){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String newPassword = encoder.encode("123456");
        User user = userService.getById(1L);
        user.setPassword(newPassword);
        userService.updateById(user);
        return Result.success();
    }
}
