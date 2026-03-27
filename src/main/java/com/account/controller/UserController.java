package com.account.controller;

import com.account.common.Result;
import com.account.dto.ChangePasswordRequest;
import com.account.dto.LoginRequest;
import com.account.dto.RegisterRequest;
import com.account.entity.User;
import com.account.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * 用户注册
     * @param request 注册请求
     * @return 用户信息
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        return Result.success("注册成功", data);
    }
    
    /**
     * 用户登录
     * @param request 登录请求
     * @return token
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        return Result.success("登录成功", data);
    }
    
    /**
     * 修改密码
     * @param token 登录token
     * @param request 修改密码请求
     * @return 结果
     */
    @PostMapping("/changePassword")
    public Result<Void> changePassword(@RequestHeader(value = "Authorization", required = false) String token,
                                        @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(token, request);
        return Result.success("密码修改成功", null);
    }
    
    /**
     * 获取用户信息
     * @param token 登录token
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        User user = userService.getUserByToken(token);
        if (user == null) {
            return Result.error(401, "用户未登录");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("createTime", user.getCreateTime());
        return Result.success(data);
    }
    
    /**
     * 登出
     * @param token 登录token
     * @return 结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        userService.logout(token);
        return Result.success("登出成功", null);
    }
}
