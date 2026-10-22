package com.hzk.loganomaly.controller;

import com.hzk.loganomaly.common.Result;
import com.hzk.loganomaly.dto.LoginDTO;
import com.hzk.loganomaly.service.AuthService;
import com.hzk.loganomaly.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

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
}
