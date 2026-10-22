package com.hzk.loganomaly.interceptor;

import com.alibaba.fastjson2.JSON;
import com.hzk.loganomaly.common.Result;
import com.hzk.loganomaly.common.utils.JwtUtil;
import com.hzk.loganomaly.common.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {


        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if(jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdByToken(token);
                UserContext.setUserId(userId);
                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=utf-8");

        Result result = Result.error(401, "未登录或登录失效");
        response.getWriter().write(JSON.toJSONString(result));

        return false;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) throws Exception {
        UserContext.clear();
    }
}
