package com.zj.aiagent.domain.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码哈希
     */
    private String passwordHash;

    /**
     * 盐值
     */
    private String salt;

    /**
     * 状态：0-禁用，1-正常
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;
    private LocalDateTime createTime;

    private Long createBy;

    private LocalDateTime updateTime;

    private Long updateBy;

    /**
     * 检查用户是否激活
     * 
     * @return true-已激活，false-已禁用
     */
    public boolean isActive() {
        return status != null && status == 1;
    }

    /**
     * 验证密码是否正确
     * 
     * @param rawPassword 原始密码
     * @return true-密码正确，false-密码错误
     */
    public boolean validatePassword(String rawPassword) {
        if (rawPassword == null || passwordHash == null || salt == null) {
            return false;
        }
        String hashedPassword = hashPassword(rawPassword, salt);
        return passwordHash.equals(hashedPassword);
    }

    /**
     * 设置密码（自动加盐加密）
     * 
     * @param rawPassword 原始密码
     */
    public void setPassword(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        this.salt = generateSalt();
        this.passwordHash = hashPassword(rawPassword, this.salt);
    }

    /**
     * 更新最后登录信息
     * 
     * @param loginIp 登录IP
     */
    public void updateLastLogin(String loginIp) {
        this.lastLoginTime = LocalDateTime.now();
        this.lastLoginIp = loginIp;
    }

    /**
     * 根据邮箱生成唯一用户名
     * 
     * @param email 邮箱地址
     * @return 生成的用户名
     */
    public static String generateUsernameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("无效的邮箱地址");
        }

        // 提取邮箱前缀作为基础用户名
        String baseUsername = email.substring(0, email.indexOf("@"));

        // 移除非法字符，只保留字母、数字和下划线
        baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9_]", "_");

        // 限制长度，最多15个字符
        if (baseUsername.length() > 15) {
            baseUsername = baseUsername.substring(0, 15);
        }

        // 添加随机后缀（4位数字）
        int randomSuffix = (int) (Math.random() * 10000);
        return baseUsername + String.format("%04d", randomSuffix);
    }

    /**
     * 生成盐值
     * 
     * @return 盐值
     */
    private static String generateSalt() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * 对密码进行哈希
     * 
     * @param password 原始密码
     * @param salt     盐值
     * @return 哈希后的密码
     */
    private static String hashPassword(String password, String salt) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            String saltedPassword = password + salt;
            byte[] hash = digest.digest(saltedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     * 
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
