package com.zj.aiagemt.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT密钥(从配置文件读取)
     */
    @Value("${jwt.secret:aiagent-default-secret-key-change-in-production-please}")
    private String secret;

    /**
     * Token过期时间(毫秒),默认7天
     */
    @Value("${jwt.expiration:604800000}")
    private Long expiration;

    /**
     * 生成JWT Token
     *
     * @param userId 用户ID
     * @return JWT Token
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();

        log.debug("生成JWT Token, userId: {}, expiryDate: {}", userId, expiryDate);
        return token;
    }

    /**
     * 解析JWT Token并返回用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     * @throws io.jsonwebtoken.JwtException 如果Token无效或过期
     */
    public Long parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.getSubject();
        log.debug("解析JWT Token成功, userId: {}", userId);
        return Long.parseLong(userId);
    }

    /**
     * 检查Token是否过期
     *
     * @param token JWT Token
     * @return true-已过期, false-未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            log.warn("检查Token过期时解析失败: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return true-有效, false-无效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }
}
