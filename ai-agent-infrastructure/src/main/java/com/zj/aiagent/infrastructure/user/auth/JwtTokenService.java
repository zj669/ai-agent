package com.zj.aiagent.infrastructure.user.auth;

import com.zj.aiagent.domain.user.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT令牌服务实现 - 基础设施层
 * 
 * 实现基于JWT的令牌生成、解析和验证
 * 这是TokenService接口的具体实现，属于技术细节
 */
@Slf4j
@Service
public class JwtTokenService implements TokenService {

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

    @Override
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

    @Override
    public Long parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            log.debug("解析JWT Token成功, userId: {}", userId);
            return Long.parseLong(userId);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("解析JWT Token失败: {}", e.getMessage());
            throw new IllegalArgumentException("无效的令牌", e);
        }
    }

    @Override
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

    @Override
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
