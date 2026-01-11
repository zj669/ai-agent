package com.zj.aiagent.application.user;

import com.zj.aiagent.application.user.dto.UserDetailDTO;
import com.zj.aiagent.application.user.dto.UserLoginResponse;
import com.zj.aiagent.application.user.dto.UserRequests;
import com.zj.aiagent.domain.auth.service.ITokenService;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.repository.IUserRepository;
import com.zj.aiagent.domain.user.service.UserAuthenticationDomainService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户应用服务测试")
class UserApplicationServiceTest {

    @Mock
    private UserAuthenticationDomainService authenticationDomainService;

    @Mock
    private ITokenService tokenService;

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private UserApplicationService applicationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(applicationService, "jwtExpirationMs", 604800000L);

        String encryptedPassword = new BCryptPasswordEncoder().encode("password");
        testUser = User.reconstruct(
                1L, "testuser", "test@example.com", encryptedPassword,
                "13800138000", "/avatar.png", UserStatus.NORMAL.getCode(),
                null, null, false,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("发送验证码测试")
    class SendEmailCodeTests {

        @Test
        @DisplayName("正常发送验证码")
        void shouldSendEmailCode() {
            // Given
            UserRequests.SendEmailCodeRequest request = new UserRequests.SendEmailCodeRequest();
            request.setEmail("test@example.com");

            // When
            applicationService.sendEmailCode(request);

            // Then
            verify(authenticationDomainService).sendVerificationCode("test@example.com");
        }
    }

    @Nested
    @DisplayName("注册测试")
    class RegisterTests {

        @Test
        @DisplayName("正常注册应返回登录响应")
        void shouldReturnLoginResponseOnSuccessfulRegistration() {
            // Given
            UserRequests.RegisterByEmailRequest request = new UserRequests.RegisterByEmailRequest();
            request.setEmail("new@example.com");
            request.setCode("123456");
            request.setPassword("password123");
            request.setUsername("newuser");

            when(authenticationDomainService.register(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(testUser);
            when(tokenService.createToken(any(User.class))).thenReturn("mock-jwt-token");

            // When
            UserLoginResponse response = applicationService.registerByEmail(request);

            // Then
            assertNotNull(response);
            assertEquals("mock-jwt-token", response.getToken());
            assertNotNull(response.getUser());
            assertEquals("testuser", response.getUser().getUsername());
        }
    }

    @Nested
    @DisplayName("登录测试")
    class LoginTests {

        @Test
        @DisplayName("正常登录应返回 Token 和用户信息")
        void shouldReturnTokenAndUserInfoOnSuccessfulLogin() {
            // Given
            UserRequests.LoginRequest request = new UserRequests.LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("password123");

            when(authenticationDomainService.login(anyString(), anyString(), anyString()))
                    .thenReturn(testUser);
            when(tokenService.createToken(any(User.class))).thenReturn("mock-jwt-token");

            // When
            UserLoginResponse response = applicationService.login(request, "192.168.1.1");

            // Then
            assertNotNull(response);
            assertEquals("mock-jwt-token", response.getToken());
            assertEquals(604800L, response.getExpireIn()); // 7 days in seconds
            assertNotNull(response.getUser());
        }
    }

    @Nested
    @DisplayName("获取用户信息测试")
    class GetUserInfoTests {

        @Test
        @DisplayName("用户存在时应返回用户详情")
        void shouldReturnUserDetailWhenUserExists() {
            // Given
            when(userRepository.findById(1L)).thenReturn(testUser);

            // When
            UserDetailDTO result = applicationService.getUserInfo(1L);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("testuser", result.getUsername());
            assertEquals("test@example.com", result.getEmail());
        }

        @Test
        @DisplayName("用户不存在时应返回 null")
        void shouldReturnNullWhenUserNotFound() {
            // Given
            when(userRepository.findById(anyLong())).thenReturn(null);

            // When
            UserDetailDTO result = applicationService.getUserInfo(999L);

            // Then
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("修改用户信息测试")
    class ModifyInfoTests {

        @Test
        @DisplayName("正常修改用户信息")
        void shouldModifyUserInfo() {
            // Given
            UserRequests.ModifyUserRequest request = new UserRequests.ModifyUserRequest();
            request.setUsername("newname");
            request.setAvatarUrl("/new-avatar.png");
            request.setPhone("13900139000");

            when(userRepository.findById(1L)).thenReturn(testUser);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            UserDetailDTO result = applicationService.modifyInfo(1L, request);

            // Then
            assertNotNull(result);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("用户不存在时应抛出异常")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            UserRequests.ModifyUserRequest request = new UserRequests.ModifyUserRequest();
            when(userRepository.findById(anyLong())).thenReturn(null);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> applicationService.modifyInfo(999L, request));
        }
    }

    @Nested
    @DisplayName("登出测试")
    class LogoutTests {

        @Test
        @DisplayName("正常登出应使 Token 失效")
        void shouldInvalidateToken() {
            // When
            applicationService.logout("Bearer mock-token");

            // Then
            verify(tokenService).invalidateToken("mock-token");
        }

        @Test
        @DisplayName("Token 为 null 时不应调用失效方法")
        void shouldNotInvalidateNullToken() {
            // When
            applicationService.logout(null);

            // Then
            verify(tokenService, never()).invalidateToken(anyString());
        }
    }
}
