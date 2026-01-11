package com.zj.aiagent.domain.user.valobj;

import lombok.Getter;
import lombok.ToString;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

/**
 * 密码凭证值对象
 */
@Getter
@ToString
public class Credential {
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final String encryptedPassword;

    public Credential(String encryptedPassword) {
        Assert.hasText(encryptedPassword, "Password cannot be empty");
        this.encryptedPassword = encryptedPassword;
    }

    /**
     * 创建新的凭证（加密原始密码）
     */
    public static Credential create(String rawPassword) {
        Assert.hasText(rawPassword, "Raw password cannot be empty");
        return new Credential(ENCODER.encode(rawPassword));
    }

    /**
     * 从已有的加密密码重建凭证
     */
    public static Credential fromEncrypted(String encryptedPassword) {
        return new Credential(encryptedPassword);
    }

    /**
     * 验证密码是否匹配
     */
    public boolean verify(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return false;
        }
        return ENCODER.matches(rawPassword, this.encryptedPassword);
    }
}
