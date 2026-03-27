package com.account.service;

import com.account.common.BusinessException;
import com.account.config.CsvProperties;
import com.account.dto.ChangePasswordRequest;
import com.account.dto.LoginRequest;
import com.account.dto.RegisterRequest;
import com.account.entity.User;
import com.account.util.CsvStorageUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户服务类
 */
@Service
public class UserService {
    
    private final CsvStorageUtil csvStorageUtil;
    private final CsvProperties csvProperties;
    
    private final ConcurrentHashMap<String, User> sessionStore = new ConcurrentHashMap<>();
    
    public UserService(CsvStorageUtil csvStorageUtil, CsvProperties csvProperties) {
        this.csvStorageUtil = csvStorageUtil;
        this.csvProperties = csvProperties;
    }
    
    /**
     * 用户注册
     */
    public User register(RegisterRequest request) {
        List<String[]> users = csvStorageUtil.readAll(csvProperties.getUsersFile());
        
        for (String[] userArr : users) {
            if (userArr[1].equals(request.getUsername())) {
                throw new BusinessException("用户名已存在");
            }
        }
        
        String id = csvStorageUtil.generateId();
        String createTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String nickname = request.getNickname() != null ? request.getNickname() : "用户" + id.substring(id.length() - 4);
        
        User user = new User(id, request.getUsername(), request.getPassword(), nickname, createTime);
        csvStorageUtil.writeLine(csvProperties.getUsersFile(), user.toArray());
        
        return user;
    }
    
    /**
     * 用户登录
     */
    public String login(LoginRequest request) {
        List<String[]> users = csvStorageUtil.readAll(csvProperties.getUsersFile());
        
        User user = null;
        for (String[] userArr : users) {
            if (userArr[1].equals(request.getUsername())) {
                user = User.fromArray(userArr);
                break;
            }
        }
        
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        if (!user.getPassword().equals(request.getPassword())) {
            throw new BusinessException("密码错误");
        }
        
        String token = UUID.randomUUID().toString().replace("-", "");
        sessionStore.put(token, user);
        
        return token;
    }
    
    /**
     * 修改密码
     */
    public void changePassword(String token, ChangePasswordRequest request) {
        User user = getUserByToken(token);
        if (user == null) {
            throw new BusinessException(401, "用户未登录");
        }
        
        if (!user.getPassword().equals(request.getOldPassword())) {
            throw new BusinessException("旧密码错误");
        }
        
        user.setPassword(request.getNewPassword());
        csvStorageUtil.updateLine(csvProperties.getUsersFile(), 0, user.getId(), user.toArray());
        
        sessionStore.put(token, user);
    }
    
    /**
     * 根据token获取用户
     */
    public User getUserByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return sessionStore.get(token);
    }
    
    /**
     * 根据ID获取用户
     */
    public User getUserById(String userId) {
        String[] userArr = csvStorageUtil.findById(csvProperties.getUsersFile(), 0, userId);
        return User.fromArray(userArr);
    }
    
    /**
     * 登出
     */
    public void logout(String token) {
        if (token != null) {
            sessionStore.remove(token);
        }
    }
}
