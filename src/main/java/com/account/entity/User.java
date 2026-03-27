package com.account.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户实体
 */
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String username;
    private String password;
    private String nickname;
    private String createTime;
    
    public User() {}
    
    public User(String id, String username, String password, String nickname, String createTime) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.createTime = createTime;
    }
    
    public String[] toArray() {
        return new String[]{id, username, password, nickname, createTime};
    }
    
    public static User fromArray(String[] arr) {
        if (arr == null || arr.length < 5) {
            return null;
        }
        User user = new User();
        user.setId(arr[0]);
        user.setUsername(arr[1]);
        user.setPassword(arr[2]);
        user.setNickname(arr[3]);
        user.setCreateTime(arr[4]);
        return user;
    }
}
