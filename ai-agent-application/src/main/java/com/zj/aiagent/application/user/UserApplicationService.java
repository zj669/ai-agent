package com.zj.aiagent.application.user;

import com.zj.aiagent.application.user.dto.TokenRefreshResponse;
import com.zj.aiagent.application.user.dto.UserDetailDTO;
import com.zj.aiagent.application.user.dto.UserLoginResponse;
import com.zj.aiagent.application.user.dto.UserRequests;
import com.zj.aiagent.domain.auth.service.ITokenService;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.valobj.UserStatus;
import com.zj.aiagent.domain.user.exception.AuthenticationException;
import com.zj.aiagent.domain.user.repository.IUserRepository;
import com.zj.aiagent.domain.user.service.UserAuthenticationDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 用户应用服务
 *
 * 负责用例编排、事务边界、DTO 转换
 * 不包含业务逻辑，仅协调领域服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserAuthenticationDomainService authenticationDomainService;
    private final ITokenService tokenService;
    private final IUserRepository userRepository;

    @Value("${jwt.access-token.expiration:7200000}")
    private long accessTokenExpirationMs;

    /**
     * 发送邮箱验证码
     */
    public void sendEmailCode(UserRequests.SendEmailCodeRequest request) {
        log.info("Sending email code to: {}", request.getEmail());
        authenticationDomainService.sendVerificationCode(request.getEmail());
    }

    /**
     * 邮箱注册
     */
    @Transactional
    public UserLoginResponse registerByEmail(UserRequests.RegisterByEmailRequest request) {
        log.info("Registering user with email: {}", request.getEmail());
        User user = authenticationDomainService.register(
                request.getEmail(),
                request.getCode(),
                request.getPassword(),
                request.getUsername());
        return buildLoginResponse(user, request.getDeviceId());
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @param ip      客户端IP
     */
    @Transactional
    public UserLoginResponse login(UserRequests.LoginRequest request, String ip) {
        log.info("User login attempt: {} from IP: {}", request.getEmail(), ip);
        User user = authenticationDomainService.login(
                request.getEmail(),
                request.getPassword(),
                ip);
        return buildLoginResponse(user, request.getDeviceId());
    }

    /**
     * 刷新 Token（支持多设备）
     *
     * @param refreshToken Refresh Token
     * @param deviceId 设备ID
     * @return Token刷新响应
     */
    public TokenRefreshResponse refreshToken(String refreshToken, String deviceId) {
        log.info("[UserApp] Refreshing token for device: {}", deviceId);

        // 1. 验证 Refresh Token
        if (!tokenService.validateRefreshToken(refreshToken, deviceId)) {
            log.warn("[UserApp] Invalid refresh token");
            throw new AuthenticationException(AuthenticationException.ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. 获取用户ID
        Long userId = tokenService.getUserIdFromRefreshToken(refreshToken);
        if (userId == null) {
            log.warn("[UserApp] Failed to extract user ID from refresh token");
            throw new AuthenticationException(AuthenticationException.ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. 查询用户
        User user = userRepository.findById(userId);
        if (user == null) {
            log.warn("[UserApp] User not found: {}", userId);
            throw new AuthenticationException(AuthenticationException.ErrorCode.USER_NOT_FOUND);
        }

        // 4. 检查用户状态
        if (user.getStatus() == UserStatus.DISABLED) {
            log.warn("[UserApp] User is disabled: {}", userId);
            throw new AuthenticationException(AuthenticationException.ErrorCode.USER_DISABLED);
        }

        // 5. 生成新的 Access Token 和 Refresh Token
        String newAccessToken = tokenService.createToken(user);
        String newRefreshToken = tokenService.createRefreshToken(user, deviceId);

        log.info("[UserApp] Token refreshed successfully for user: {}, device: {}", userId, deviceId);

        // 6. 返回响应
        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(accessTokenExpirationMs / 1000) // 转换为秒
                .tokenType("Bearer")
                .build();
    }

    /**
     * 获取用户信息
     */
    @Transactional(readOnly = true)
    public UserDetailDTO getUserInfo(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            log.warn("User not found: {}", userId);
            return null;
        }
        return toDetailDTO(user);
    }

    /**
     * 修改用户信息
     */
    @Transactional
    public UserDetailDTO modifyInfo(Long userId, UserRequests.ModifyUserRequest request) {
        User user = userRepository.findById(userId);
        if (user == null) {
            log.warn("User not found for modification: {}", userId);
            throw new IllegalArgumentException("用户不存在");
        }
        user.modifyInfo(request.getUsername(), request.getAvatarUrl(), request.getPhone());
        userRepository.save(user);
        log.info("User info modified: {}", userId);
        return toDetailDTO(user);
    }

    /**
     * 用户登出（支持多设备）
     */
    public void logout(String token, String deviceId) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null) {
            Long userId = tokenService.getUserIdFromToken(token);

            // 使 Access Token 失效
            tokenService.invalidateToken(token);

            // 使当前设备的 Refresh Token 失效
            if (userId != null && deviceId != null) {
                tokenService.invalidateRefreshToken(userId, deviceId);
            }

            log.info("[UserApp] User logged out: {}, device: {}", userId, deviceId);
        }
    }

    /**
     * 构建登录响应（支持多设备）
     */
    private UserLoginResponse buildLoginResponse(User user, String deviceId) {
        // 生成 Device ID（如果未提供）
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = UUID.randomUUID().toString();
        }

        String token = tokenService.createToken(user);
        String refreshToken = tokenService.createRefreshToken(user, deviceId);
        long expireInSeconds = accessTokenExpirationMs / 1000;

        return UserLoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expireIn(expireInSeconds)
                .deviceId(deviceId)
                .user(toDetailDTO(user))
                .build();
    }

    /**
     * 领域对象转 DTO
     */
    private UserDetailDTO toDetailDTO(User user) {
        return UserDetailDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail().getValue())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().getCode())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(UserRequests.ResetPasswordRequest request) {
        log.info("Resetting password for email: {}", request.getEmail());
        authenticationDomainService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword(),
                request.getConfirmPassword());
    }
}
