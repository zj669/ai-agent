package com.zj.aiagent.application.user;

import com.zj.aiagent.application.user.dto.UserDetailDTO;
import com.zj.aiagent.application.user.dto.UserLoginResponse;
import com.zj.aiagent.application.user.dto.UserRequests;
import com.zj.aiagent.domain.auth.service.ITokenService;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.repository.IUserRepository;
import com.zj.aiagent.domain.user.service.UserAuthenticationDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Value("${jwt.expiration:604800000}")
    private long jwtExpirationMs;

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
        return buildLoginResponse(user);
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
        return buildLoginResponse(user);
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
     * 用户登出
     */
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null) {
            tokenService.invalidateToken(token);
            log.info("User logged out");
        }
    }

    /**
     * 构建登录响应
     */
    private UserLoginResponse buildLoginResponse(User user) {
        String token = tokenService.createToken(user);
        long expireInSeconds = jwtExpirationMs / 1000;
        return UserLoginResponse.builder()
                .token(token)
                .expireIn(expireInSeconds)
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
}
