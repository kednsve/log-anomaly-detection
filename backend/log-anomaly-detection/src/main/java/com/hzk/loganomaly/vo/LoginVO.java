package com.hzk.loganomaly.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;

    private Long id;

    private String username;

    private String nickname;

    private String role;

}
