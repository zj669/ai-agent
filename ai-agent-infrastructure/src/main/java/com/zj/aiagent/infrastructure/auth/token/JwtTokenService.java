package com.zj.aiagent.infrastructure.auth.token;

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
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService implements ITokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:604800000}") // 7 days in milliseconds
    private long expirationMs;

    // Issuer
    private static final String ISSUER = "ai-agent";

    @Override
    public String createToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        Instant now = Instant.now();
        Instant exp = now.plusMillis(expirationMs);

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail().getValue())
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
            // 1. 黑名单校验 (先查 Redis 避免昂贵的验签操作，或者后查也可以)
            // 这里选择后查，确保 Token 格式正确后再查 Redis

            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);

            // 2. 检查 Redis 黑名单
            String key = RedisKeyConstants.User.TOKEN_BLACKLIST_PREFIX + token;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                log.warn("Token validation failed: Token is in blacklist");
                return false;
            }

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
}
