package com.zj.aiagent.domain.user.service;

/**
 * Token服务接口 - 领域层
 * 
 * 定义用户身份令牌的生成、解析和验证能力
 * 具体实现由基础设施层提供（如JWT、OAuth等）
 */
public interface TokenService {

    /**
     * 生成用户令牌
     * 
     * @param userId 用户ID
     * @return 令牌字符串
     */
    String generateToken(Long userId);

    /**
     * 解析令牌并返回用户ID
     * 
     * @param token 令牌字符串
     * @return 用户ID
     * @throws IllegalArgumentException 如果令牌无效或已过期
     */
    Long parseToken(String token);

    /**
     * 验证令牌是否有效
     * 
     * @param token 令牌字符串
     * @return true-有效, false-无效
     */
    boolean validateToken(String token);

    /**
     * 检查令牌是否过期
     * 
     * @param token 令牌字符串
     * @return true-已过期, false-未过期
     */
    boolean isTokenExpired(String token);

    /**
     * 使令牌失效（用于退出登录）
     * 
     * @param token 令牌字符串
     */
    void invalidateToken(String token);
}
