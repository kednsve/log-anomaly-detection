package com.hzk.loganomaly.common.exception;

import com.hzk.loganomaly.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(value = BindException.class)
    public Result handleBindException(BindException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验异常：{}", msg);
        return Result.error(msg);
    }

    @ExceptionHandler(value = Exception.class)
    public Result handleException(Exception e) {
        log.warn("系统异常：{}", e.getMessage());
        return Result.error(e.getMessage());
    }
}
