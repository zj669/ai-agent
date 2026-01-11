package com.zj.aiagent.infrastructure.auth.token;

import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.valobj.UserStatus;
import com.zj.aiagent.shared.constants.RedisKeyConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token 服务测试")
class JwtTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtTokenService tokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        tokenService = new JwtTokenService(redisTemplate);
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret-key-for-jwt-testing-purpose");
        ReflectionTestUtils.setField(tokenService, "expirationMs", 3600000L); // 1 hour

        // 使用正确的 reconstruct 方法签名
        String encryptedPassword = new BCryptPasswordEncoder().encode("password");
        testUser = User.reconstruct(
                1L, // id
                "testuser", // username
                "test@example.com", // emailStr
                encryptedPassword, // encryptedPassword
                null, // phone
                null, // avatarUrl
                UserStatus.NORMAL.getCode(), // statusCode
                null, // lastLoginIp
                null, // lastLoginTime
                false, // deleted
                LocalDateTime.now(), // createdAt
                LocalDateTime.now() // updatedAt
        );
    }

    @Test
    @DisplayName("创建 Token 应返回有效 JWT")
    void shouldCreateValidToken() {
        // When
        String token = tokenService.createToken(testUser);

        // Then
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length); // JWT format: header.payload.signature
    }

    @Test
    @DisplayName("验证有效 Token 应返回 true")
    void shouldValidateValidToken() {
        // Given
        String token = tokenService.createToken(testUser);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // When
        boolean result = tokenService.validateToken(token);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("验证无效 Token 应返回 false")
    void shouldNotValidateInvalidToken() {
        // When
        boolean result = tokenService.validateToken("invalid.jwt.token");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("验证黑名单中的 Token 应返回 false")
    void shouldNotValidateBlacklistedToken() {
        // Given
        String token = tokenService.createToken(testUser);
        when(redisTemplate.hasKey(RedisKeyConstants.User.TOKEN_BLACKLIST_PREFIX + token)).thenReturn(true);

        // When
        boolean result = tokenService.validateToken(token);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("从 Token 获取用户 ID 应返回正确值")
    void shouldGetUserIdFromToken() {
        // Given
        String token = tokenService.createToken(testUser);

        // When
        Long userId = tokenService.getUserIdFromToken(token);

        // Then
        assertEquals(1L, userId);
    }

    @Test
    @DisplayName("从无效 Token 获取用户 ID 应返回 null")
    void shouldReturnNullForInvalidToken() {
        // When
        Long userId = tokenService.getUserIdFromToken("invalid.token");

        // Then
        assertNull(userId);
    }

    @Test
    @DisplayName("使 Token 失效应将其加入 Redis 黑名单")
    void shouldAddTokenToBlacklistWhenInvalidating() {
        // Given
        String token = tokenService.createToken(testUser);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        tokenService.invalidateToken(token);

        // Then
        verify(valueOperations).set(
                eq(RedisKeyConstants.User.TOKEN_BLACKLIST_PREFIX + token),
                eq("invalid"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS));
    }
}
