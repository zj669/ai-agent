package com.zj.aiagent.domain.user.entity;

import com.zj.aiagent.domain.user.valobj.Credential;
import com.zj.aiagent.domain.user.valobj.Email;
import com.zj.aiagent.domain.user.valobj.UserStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 用户聚合根
 */
@Getter
@ToString
@NoArgsConstructor
public class User {
    private Long id;
    private String username;
    private Email email;
    private Credential credential;
    private String phone;
    private String avatarUrl;
    private UserStatus status;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(String username, Email email, Credential credential) {
        this.username = username;
        this.email = email;
        this.credential = credential;
        this.status = UserStatus.NORMAL;
        this.deleted = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 重建对象使用
    public static User reconstruct(Long id, String username, String emailStr, String encryptedPassword,
            String phone, String avatarUrl, Integer statusCode,
            String lastLoginIp, LocalDateTime lastLoginTime,
            Boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        User user = new User();
        user.id = id;
        user.username = username;
        user.email = Email.of(emailStr);
        user.credential = Credential.fromEncrypted(encryptedPassword);
        user.phone = phone;
        user.avatarUrl = avatarUrl;
        user.status = UserStatus.fromCode(statusCode);
        user.lastLoginIp = lastLoginIp;
        user.lastLoginTime = lastLoginTime;
        user.deleted = deleted;
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        return user;
    }

    /**
     * 修改个人信息
     */
    public void modifyInfo(String username, String avatarUrl, String phone) {
        if (username != null && !username.isBlank()) {
            this.username = username;
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            this.avatarUrl = avatarUrl;
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 验证密码
     */
    public boolean verifyPassword(String rawPassword) {
        return this.credential.verify(rawPassword);
    }

    /**
     * 记录登录成功
     */
    public void onLoginSuccess(String ip) {
        this.lastLoginIp = ip;
        this.lastLoginTime = LocalDateTime.now();
    }
}
