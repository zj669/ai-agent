package com.zj.aiagent.domain.auth.service;

import com.zj.aiagent.domain.user.entity.User;

public interface ITokenService {
    String createToken(User user);

    void invalidateToken(String token);

    boolean validateToken(String token);

    Long getUserIdFromToken(String token);

    /**
     * 创建 Refresh Token（支持多设备）
     *
     * @param user 用户实体
     * @param deviceId 设备ID
     * @return Refresh Token字符串
     */
    String createRefreshToken(User user, String deviceId);

    /**
     * 验证 Refresh Token（支持多设备）
     *
     * @param refreshToken Refresh Token字符串
     * @param deviceId 设备ID
     * @return true-有效, false-无效
     */
    boolean validateRefreshToken(String refreshToken, String deviceId);

    /**
     * 从 Refresh Token 中获取用户ID
     *
     * @param refreshToken Refresh Token字符串
     * @return 用户ID
     */
    Long getUserIdFromRefreshToken(String refreshToken);

    /**
     * 使 Refresh Token 失效（指定设备）
     *
     * @param userId 用户ID
     * @param deviceId 设备ID
     */
    void invalidateRefreshToken(Long userId, String deviceId);

    /**
     * 使用户所有设备的 Refresh Token 失效
     *
     * @param userId 用户ID
     */
    void invalidateAllRefreshTokens(Long userId);
}
