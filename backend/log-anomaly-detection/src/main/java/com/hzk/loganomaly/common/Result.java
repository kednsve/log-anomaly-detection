package com.hzk.loganomaly.common;

import lombok.Data;

@Data
public class Result {
    private Integer code;
    private String msg;
    private Object data;

    public Result(Integer code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static Result success(Object data) {
        return new Result(200, "success", data);
    }

    public static Result success() {
        return new Result(200, "success", null);
    }

    public static Result error() {
        return new Result(400, "error", null);
    }

    public static Result error(String msg) {
        return new Result(400, msg, null);
    }

    public static Result error(Integer code, String msg) {
        return new Result(code, msg, null);
    }
}