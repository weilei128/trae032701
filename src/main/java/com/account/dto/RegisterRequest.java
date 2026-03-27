package com.account.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 用户注册请求
 */
@Data
public class RegisterRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^(1[3-9]\\d{9})|([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})$", 
             message = "用户名必须是手机号或邮箱格式")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;
    
    @Size(max = 20, message = "昵称最长20个字符")
    private String nickname;
}
