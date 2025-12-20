package com.zj.aiagemt.utils;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码加密工具类
 */
@Slf4j
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
            log.error("SHA-256算法不可用", e);
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
