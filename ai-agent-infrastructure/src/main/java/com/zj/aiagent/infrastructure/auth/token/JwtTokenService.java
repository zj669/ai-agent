package com.zj.aiagent.infrastructure.auth.token;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.zj.aiagent.domain.auth.service.ITokenService;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.shared.constants.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService implements ITokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token.expiration:7200000}") // 2 hours
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token.expiration:604800000}") // 7 days
    private long refreshTokenExpirationMs;

    // Issuer
    private static final String ISSUER = "ai-agent";

    @Override
    public String createToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        Instant now = Instant.now();
        Instant exp = now.plusMillis(accessTokenExpirationMs);

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail().getValue())
                .withClaim("type", "access") // 标识为 Access Token
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .sign(algorithm);
    }

    @Override
    public void invalidateToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            long now = System.currentTimeMillis();
            long exp = jwt.getExpiresAt().getTime();
            long ttl = exp - now;

            if (ttl > 0) {
                String key = RedisKeyConstants.User.TOKEN_BLACKLIST_PREFIX + token;
                // 将 Token 加入黑名单，值设为 invalid，过期时间设为 Token 剩余有效期
                redisTemplate.opsForValue().set(key, "invalid", ttl, TimeUnit.MILLISECONDS);
                log.info("Token invalidated (added to blacklist): {}", token);
            } else {
                log.info("Token already expired, skipping blacklist: {}", token);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate token: {}", e.getMessage());
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            // 1. 先检查 Redis 黑名单 (避免昂贵的验签操作)
            String key = RedisKeyConstants.User.TOKEN_BLACKLIST_PREFIX + token;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                log.warn("Token validation failed: Token is in blacklist");
                return false;
            }

            // 2. JWT 签名验证
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);

            return true;
        } catch (Exception e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Long getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return Long.parseLong(jwt.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String createRefreshToken(User user, String deviceId) {
        // 如果未提供 deviceId，生成一个
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = UUID.randomUUID().toString();
        }

        Algorithm algorithm = Algorithm.HMAC256(secret);
        Instant now = Instant.now();
        Instant exp = now.plusMillis(refreshTokenExpirationMs);

        String token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getId().toString())
                .withClaim("type", "refresh")
                .withClaim("email", user.getEmail().getValue())
                .withClaim("deviceId", deviceId)  // 设备ID
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(exp))
                .sign(algorithm);

        // 存储到 Redis（多设备）
        storeRefreshToken(user.getId(), deviceId, token, exp);

        return token;
    }

    @Override
    public boolean validateRefreshToken(String refreshToken, String deviceId) {
        try {
            // 1. JWT 签名验证
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withClaim("type", "refresh")
                    .build()
                    .verify(refreshToken);

            // 2. 提取用户ID和设备ID
            Long userId = Long.parseLong(jwt.getSubject());
            String tokenDeviceId = jwt.getClaim("deviceId").asString();

            // 3. 验证设备ID匹配
            if (deviceId != null && !deviceId.equals(tokenDeviceId)) {
                log.warn("Device ID mismatch for user: {}", userId);
                return false;
            }

            // 4. 检查 Redis 中是否存在
            String key = RedisKeyConstants.User.REFRESH_TOKEN_PREFIX + userId + ":" + tokenDeviceId;
            String storedData = redisTemplate.opsForValue().get(key);

            if (storedData == null) {
                log.warn("Refresh token not found in Redis for user: {}, device: {}", userId, tokenDeviceId);
                return false;
            }

            // 5. 验证 token 是否匹配
            Map<String, Object> data = JSON.parseObject(storedData, new TypeReference<Map<String, Object>>() {});
            String storedToken = (String) data.get("token");

            if (!storedToken.equals(refreshToken)) {
                log.warn("Refresh token mismatch for user: {}, device: {}", userId, tokenDeviceId);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Long getUserIdFromRefreshToken(String refreshToken) {
        try {
            DecodedJWT jwt = JWT.decode(refreshToken);
            return Long.parseLong(jwt.getSubject());
        } catch (Exception e) {
            log.warn("Failed to extract user ID from refresh token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void invalidateRefreshToken(Long userId, String deviceId) {
        String key = RedisKeyConstants.User.REFRESH_TOKEN_PREFIX + userId + ":" + deviceId;
        redisTemplate.delete(key);
        log.info("Refresh token invalidated for user: {}, device: {}", userId, deviceId);
    }

    @Override
    public void invalidateAllRefreshTokens(Long userId) {
        // 查找所有设备的 Refresh Token
        String pattern = RedisKeyConstants.User.REFRESH_TOKEN_PREFIX + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("All refresh tokens invalidated for user: {}, count: {}", userId, keys.size());
        }
    }

    /**
     * 存储 Refresh Token 到 Redis
     */
    private void storeRefreshToken(Long userId, String deviceId, String token, Instant expiresAt) {
        // 多设备 Key: refresh_token:{userId}:{deviceId}
        String key = RedisKeyConstants.User.REFRESH_TOKEN_PREFIX + userId + ":" + deviceId;

        // 存储 token 和元数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("issuedAt", Instant.now().toEpochMilli());
        data.put("expiresAt", expiresAt.toEpochMilli());
        data.put("deviceId", deviceId);

        long ttl = expiresAt.toEpochMilli() - System.currentTimeMillis();
        redisTemplate.opsForValue().set(key, JSON.toJSONString(data), ttl, TimeUnit.MILLISECONDS);

        log.info("Refresh token stored for user: {}, device: {}", userId, deviceId);
    }
}
