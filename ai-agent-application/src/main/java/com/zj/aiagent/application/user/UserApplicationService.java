package com.zj.aiagent.application.user;

import com.zj.aiagent.application.user.command.LoginCommand;
import com.zj.aiagent.application.user.command.RegisterByEmailCommand;
import com.zj.aiagent.application.user.command.RegisterUserCommand;
import com.zj.aiagent.application.user.command.SendEmailCodeCommand;
import com.zj.aiagent.application.user.query.GetUserInfoQuery;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.repository.UserRepository;
import com.zj.aiagent.domain.user.service.EmailService;
import com.zj.aiagent.domain.user.service.RateLimitService;
import com.zj.aiagent.domain.user.service.TokenService;
import com.zj.aiagent.domain.user.service.UserAuthenticationDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户应用服务
 * 
 * <p>
 * 编排用户相关业务流程，调用领域服务完成业务逻辑
 * 
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {
    private final UserAuthenticationDomainService authenticationDomainService;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;

    /**
     * 用户注册
     * 
     * @param command 注册命令
     * @return 用户信息（包含Token）
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDTO register(RegisterUserCommand command) {
        log.info("执行用户注册, username: {}", command.getUsername());

        // 1. 调用领域服务执行注册
        User user = authenticationDomainService.register(
                command.getUsername(),
                command.getPassword(),
                command.getEmail(),
                command.getPhone());

        // 2. 生成Token
        String token = tokenService.generateToken(user.getId());

        // 3. 返回用户信息
        return buildUserDTO(user, token);
    }

    /**
     * 邮箱注册
     * 
     * @param command 邮箱注册命令
     * @return 用户信息（包含Token）
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDTO registerByEmail(RegisterByEmailCommand command) {
        log.info("执行邮箱注册, email: {}", command.getEmail());

        // 1. 验证验证码
        if (!emailService.verifyCode(command.getEmail(), command.getCode())) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        // 2. 调用领域服务执行注册
        User user = authenticationDomainService.registerByEmail(
                command.getEmail(),
                command.getPassword(),
                command.getUsername(),
                command.getIp());

        // 3. 生成Token
        String token = tokenService.generateToken(user.getId());

        // 4. 返回用户信息
        return buildUserDTO(user, token);
    }

    /**
     * 用户登录
     * 
     * @param command 登录命令
     * @return 用户信息（包含Token）
     */
    @Transactional(rollbackFor = Exception.class)
    public UserDTO login(LoginCommand command) {
        log.info("执行用户登录, account: {}", command.getAccount());

        // 1. 调用领域服务执行登录
        User user = authenticationDomainService.login(
                command.getAccount(),
                command.getPassword(),
                command.getLoginIp());

        // 2. 生成Token
        String token = tokenService.generateToken(user.getId());

        // 3. 返回用户信息
        return buildUserDTO(user, token);
    }

    /**
     * 发送邮箱验证码
     * 
     * @param command 发送验证码命令
     */
    public void sendEmailCode(SendEmailCodeCommand command) {
        log.info("发送邮箱验证码, email: {}", command.getEmail());

        // 1. 执行三重限流检查
        rateLimitService.checkAllLimits(command.getEmail(), command.getIp(), command.getDeviceId());

        // 2. 发送验证码
        emailService.sendVerificationCode(command.getEmail(), command.getIp(), command.getDeviceId());
    }

    /**
     * 获取用户信息
     * 
     * @param query 查询对象
     * @return 用户信息
     */
    public UserDTO getUserInfo(GetUserInfoQuery query) {
        log.info("获取用户信息, userId: {}", query.getUserId());

        User user = userRepository.findById(query.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        return buildUserDTO(user, null);
    }

    /**
     * 用户退出登录
     * 
     * @param token 用户Token
     */
    public void logout(String token) {
        log.info("执行用户退出登录");

        // 使Token失效
        tokenService.invalidateToken(token);

        log.info("用户退出登录成功");
    }

    /**
     * 构建用户DTO
     * 
     * @param user  用户实体
     * @param token Token（可选）
     * @return 用户DTO
     */
    private UserDTO buildUserDTO(User user, String token) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .token(token)
                .build();
    }

    /**
     * 用户DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserDTO {
        private Long id;
        private String username;
        private String email;
        private String phone;
        private String token;
    }
}
