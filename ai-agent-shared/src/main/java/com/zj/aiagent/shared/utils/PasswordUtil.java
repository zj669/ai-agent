package com.zj.aiagent.shared.utils;



import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码加密工具类 - 共享内核
 * 
 * 提供密码的盐值生成、加密哈希和验证功能
 * 使用SHA-256算法 + 随机盐值，确保密码安全存储
 * 
 * <h3>使用示例：</h3>
 * 
 * <pre>{@code
 * // 用户注册时
 * String salt = PasswordUtil.generateSalt();
 * String passwordHash = PasswordUtil.hashPassword(rawPassword, salt);
 * user.setSalt(salt);
 * user.setPasswordHash(passwordHash);
 * 
 * // 用户登录时
 * boolean isValid = PasswordUtil.verifyPassword(
 *         inputPassword,
 *         user.getSalt(),
 *         user.getPasswordHash());
 * }</pre>
 * 
 * <h3>安全建议：</h3>
 * <ul>
 * <li>每个用户使用独立的随机盐值</li>
 * <li>盐值和密码哈希都需要存储到数据库</li>
 * <li>不要在日志中输出原始密码</li>
 * <li>考虑使用BCrypt、Argon2等更安全的哈希算法（未来优化）</li>
 * </ul>
 * 
 * @see java.security.MessageDigest
 * @see java.security.SecureRandom
 */
public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_LENGTH = 16;

    /**
     * 生成随机盐值
     *
     * @return Base64编码的盐值
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 使用SHA-256加密密码
     *
     * @param password 原始密码
     * @param salt     盐值
     * @return 加密后的密码(十六进制字符串)
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedPassword = password + salt;
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 验证密码
     *
     * @param password 原始密码
     * @param salt     盐值
     * @param hash     已加密的密码
     * @return true-密码正确, false-密码错误
     */
    public static boolean verifyPassword(String password, String salt, String hash) {
        String computedHash = hashPassword(password, salt);
        return computedHash.equals(hash);
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
