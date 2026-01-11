package com.zj.aiagent.domain.user.service;

import com.zj.aiagent.domain.auth.service.ratelimit.RateLimiter;
import com.zj.aiagent.domain.auth.service.ratelimit.RateLimiterFactory;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.exception.AuthenticationException;
import com.zj.aiagent.domain.user.repository.IUserRepository;
import com.zj.aiagent.domain.user.repository.IVerificationCodeRepository;
import com.zj.aiagent.domain.user.valobj.Email;
import com.zj.aiagent.domain.user.valobj.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户认证领域服务测试")
class UserAuthenticationDomainServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IEmailService emailService;

    @Mock
    private IVerificationCodeRepository verificationCodeRepository;

    @Mock
    private RateLimiterFactory rateLimiterFactory;

    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private UserAuthenticationDomainService domainService;

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User createTestUser(String email, String rawPassword, UserStatus status) {
        String encryptedPassword = passwordEncoder.encode(rawPassword);
        return User.reconstruct(
                1L, "testuser", email, encryptedPassword,
                null, null, status.getCode(),
                null, null, false,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("发送验证码测试")
    class SendVerificationCodeTests {

        @BeforeEach
        void setUp() {
            when(rateLimiterFactory.getDefaultLimiter()).thenReturn(rateLimiter);
        }

        @Test
        @DisplayName("正常发送验证码")
        void shouldSendVerificationCodeSuccessfully() {
            // Given
            String email = "test@example.com";
            when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
            when(emailService.sendVerificationCode(any(Email.class), anyString())).thenReturn(true);

            // When
            assertDoesNotThrow(() -> domainService.sendVerificationCode(email));

            // Then
            verify(verificationCodeRepository).save(any(Email.class), anyString(), eq(300L));
            verify(emailService).sendVerificationCode(any(Email.class), anyString());
        }

        @Test
        @DisplayName("发送频率过高应被限流")
        void shouldThrowExceptionWhenRateLimited() {
            // Given
            String email = "test@example.com";
            when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(false);

            // When & Then
            AuthenticationException exception = assertThrows(
                    AuthenticationException.class,
                    () -> domainService.sendVerificationCode(email));
            assertEquals(AuthenticationException.ErrorCode.RATE_LIMITED, exception.getErrorCode());
            verify(emailService, never()).sendVerificationCode(any(), anyString());
        }

        @Test
        @DisplayName("邮件发送失败应抛出异常")
        void shouldThrowExceptionWhenEmailSendFails() {
            // Given
            String email = "test@example.com";
            when(rateLimiter.tryAcquire(anyString(), anyInt(), anyInt())).thenReturn(true);
            when(emailService.sendVerificationCode(any(Email.class), anyString())).thenReturn(false);

            // When & Then
            AuthenticationException exception = assertThrows(
                    AuthenticationException.class,
                    () -> domainService.sendVerificationCode(email));
            assertEquals(AuthenticationException.ErrorCode.EMAIL_SEND_FAILED, exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("用户注册测试")
    class RegisterTests {

        @Test
        @DisplayName("正常注册流程")
        void shouldRegisterSuccessfully() {
            // Given
            String email = "newuser@example.com";
            String code = "123456";
            String password = "password123";
            String username = "新用户";

            when(verificationCodeRepository.get(any(Email.class))).thenReturn(code);
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            User result = domainService.register(email, code, password, username);

            // Then
            assertNotNull(result);
            assertEquals(username, result.getUsername());
            assertEquals(email, result.getEmail().getValue());
            verify(verificationCodeRepository).remove(any(Email.class));
        }

        @Test
        @DisplayName("验证码无效应抛出异常")
        void shouldThrowExceptionWhenInvalidCode() {
            // Given
            String email = "test@example.com";
            when(verificationCodeRepository.get(any(Email.class))).thenReturn("999999");

            // When & Then
            AuthenticationException exception = assertThrows(
                    AuthenticationException.class,
                    () -> domainService.register(email, "123456", "password123", "user"));
            assertEquals(AuthenticationException.ErrorCode.INVALID_VERIFICATION_CODE, exception.getErrorCode());
        }

        @Test
        @DisplayName("验证码过期应抛出异常")
        void shouldThrowExceptionWhenCodeExpired() {
            // Given
            String email = "test@example.com";
            when(verificationCodeRepository.get(any(Email.class))).thenReturn(null);

            // When & Then
            AuthenticationException exception = assertThrows(
                    AuthenticationException.class,
                    () -> domainService.register(email, "123456", "password123", "user"));
            assertEquals(AuthenticationException.ErrorCode.INVALID_VERIFICATION_CODE, exception.getErrorCode());
        }

        @Test
        @DisplayName("邮箱已注册应抛出异常")
        void shouldThrowExceptionWhenEmailAlreadyRegistered() {
            // Given
            String email = "existing@example.com";
            String code = "123456";
            when(verificationCodeRepository.get(any(Email.class))).thenReturn(code);
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

            // When & Then
            AuthenticationException exception = assertThrows(
                    AuthenticationException.class,
                    () -> domainService.register(email, code, "password123", "user"));
            assertEquals(AuthenticationException.ErrorCode.EMAIL_ALREADY_REGISTERED, exception.getErrorCode());
        }

        @Test
        @DisplayName("密码太短应抛出异常")
        void shouldThrowExceptionWhenPasswordTooShort() {
            // Given
            String email = "test@example.com";
            String code = "123456";
            when(verificationCodeRepository.get(any(Email.class))).thenReturn(code);
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);

            // When & Then
            AuthenticationException exception = assertThrows(
                    AuthenticationException.class,
                    () -> domainService.register(email, code, "short", "user"));
            assertEquals(AuthenticationException.ErrorCode.WEAK_PASSWORD, exception.getErrorCode());
        }

        @Test
        @DisplayName("未提供用户名时使用邮箱前缀")
        void shouldUseEmailPrefixAsUsernameWhenNotProvided() {
            // Given
            String email = "testuser@example.com";
            String code = "123456";
            when(verificationCodeRepository.get(any(Email.class))).thenReturn(code);
            when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            User result = domainService.register(email, code, "password123", null);

            // Then
            assertEquals("testuser", result.getUsername());
        }
    }

    @Nested
    @DisplayName("用户登录测试")
    class LoginTests {

        @Test
        @DisplayName("正常登录流程")
        void shouldLoginSuccessfully() {
            // Given
            String email = "test@example.com";
            String password = "password123";
            String ip = "192.168.1.1";
            User user = createTestUser(email, password, UserStatus.NORMAL);

            when(userRepository.findByEmail(any(Email.class))).thenReturn(user);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            User result = domainService.login(email, password, ip);

            // Then
            assertNotNull(result);
            assertEquals(ip, result.getLastLoginIp());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("用户不存在应抛出异常")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(userRepository.findByEmail(any(Email.class))).thenReturn(null);

            // When & Then
            AuthenticationException exception = assertThrows(
                    AuthenticationException.class,
                    () -> domainService.login("notfound@example.com", "password", "127.0.0.1"));
            assertEquals(AuthenticationException.ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        }

        @Test
        @DisplayName("密码错误应抛出异常")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            // Given
            String email = "test@example.com";
            User user = createTestUser(email, "correctPassword", UserStatus.NORMAL);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(user);

            // When & Then
            AuthenticationException exception = assertThrows(
                    AuthenticationException.class,
                    () -> domainService.login(email, "wrongPassword", "127.0.0.1"));
            assertEquals(AuthenticationException.ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        }

        @Test
        @DisplayName("用户被禁用应抛出异常")
        void shouldThrowExceptionWhenUserDisabled() {
            // Given
            String email = "disabled@example.com";
            String password = "password123";
            User user = createTestUser(email, password, UserStatus.DISABLED);
            when(userRepository.findByEmail(any(Email.class))).thenReturn(user);

            // When & Then
            AuthenticationException exception = assertThrows(
                    AuthenticationException.class,
                    () -> domainService.login(email, password, "127.0.0.1"));
            assertEquals(AuthenticationException.ErrorCode.USER_DISABLED, exception.getErrorCode());
        }
    }
}
