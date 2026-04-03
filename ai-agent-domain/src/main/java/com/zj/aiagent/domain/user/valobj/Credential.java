package com.zj.aiagent.domain.user.valobj;

import com.zj.aiagent.shared.util.BCryptUtil;
import lombok.Getter;
import lombok.ToString;

/**
 * 密码凭证值对象
 * 纯 POJO，不依赖任何框架。加密操作由调用方（领域服务/应用服务）负责。
 */
@Getter
@ToString
public class Credential {

    private final String encryptedPassword;

    /**
     * 内部构造器（供 fromEncrypted 使用）
     */
    Credential(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    /**
     * 从加密后的密码创建凭证（从数据库重建时使用）
     */
    public static Credential fromEncrypted(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isBlank()) {
            throw new IllegalArgumentException("Encrypted password cannot be empty");
        }
        return new Credential(encryptedPassword);
    }

    /**
     * 验证密码是否匹配
     */
    public boolean verify(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return false;
        }
        return BCryptUtil.matches(rawPassword, this.encryptedPassword);
    }
}
