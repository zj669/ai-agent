package com.zj.aiagent.shared.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt 密码加密工具类
 * <p>
 * 放在 shared 模块，避免领域层依赖 Spring Security 框架。
 * jBCrypt 是纯 Java 实现的 BCrypt 算法，无任何框架依赖。
 */
public final class BCryptUtil {

    private BCryptUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 对原始密码进行 BCrypt 加密
     *
     * @param rawPassword 原始密码（明文）
     * @return BCrypt 加密后的密码
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Raw password cannot be empty");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * 验证原始密码是否匹配 BCrypt 加密后的密码
     *
     * @param rawPassword     原始密码（明文）
     * @param encryptedPassword BCrypt 加密后的密码
     * @return true-匹配，false-不匹配
     */
    public static boolean matches(String rawPassword, String encryptedPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return false;
        }
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return false;
        }
        return BCrypt.checkpw(rawPassword, encryptedPassword);
    }
}
