package com.hmdp.dto;

import lombok.Data;

/**
 * 用来接收用户登录时（账号、验证码、密码）的参数对象
 * */
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
