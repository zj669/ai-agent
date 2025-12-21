package com.zj.aiagent.domain.user.service;

import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.Optional;

/**
 * 用户认证领域服务
 * 
 * <p>
 * 负责用户注册、登录等认证相关的核心业务逻辑
 * 
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationDomainService {

    private final UserRepository userRepository;

    /**
     * 用户注册
     * 
     * @param username 用户名
     * @param password 密码
     * @param email    邮箱（可选）
     * @param phone    手机号（可选）
     * @return 新创建的用户
     */
    public User register(String username, String password, String email, String phone) {
        log.info("用户注册, username: {}, email: {}", username, email);

        // 1. 校验用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 2. 校验邮箱是否已存在
        if (email != null && !email.isEmpty() && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已被注册");
        }

        // 3. 校验手机号是否已存在
        if (phone != null && !phone.isEmpty() && userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("手机号已被注册");
        }

        // 4. 创建用户实体
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(password); // 自动加盐加密
        user.setStatus(1); // 默认启用

        // 5. 保存用户
        User savedUser = userRepository.save(user);
        log.info("用户注册成功, userId: {}", savedUser.getId());

        return savedUser;
    }

    /**
     * 邮箱注册
     * 
     * @param email    邮箱
     * @param password 密码
     * @param username 用户名（可选，为空则自动生成）
     * @param loginIp  注册IP
     * @return 新创建的用户
     */
    public User registerByEmail(String email, String password, String username, String loginIp) {
        log.info("邮箱注册, email: {}, username: {}", email, username);

        // 1. 校验邮箱是否已存在
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("该邮箱已被注册");
        }

        // 2. 生成或验证用户名
        String finalUsername = username;
        if (finalUsername == null || finalUsername.isEmpty()) {
            // 根据邮箱自动生成用户名
            finalUsername = User.generateUsernameFromEmail(email);

            // 确保生成的用户名不重复（最多尝试10次）
            int attempts = 0;
            while (userRepository.existsByUsername(finalUsername) && attempts < 10) {
                finalUsername = User.generateUsernameFromEmail(email);
                attempts++;
            }

            if (userRepository.existsByUsername(finalUsername)) {
                throw new IllegalStateException("无法生成唯一用户名，请手动指定");
            }
        } else {
            // 检查用户名是否已存在
            if (userRepository.existsByUsername(finalUsername)) {
                throw new IllegalArgumentException("用户名已存在");
            }
        }

        // 3. 创建用户实体
        User user = new User();
        user.setUsername(finalUsername);
        user.setEmail(email);
        user.setPassword(password); // 自动加盐加密
        user.setStatus(1); // 默认启用
        user.updateLastLogin(loginIp); // 设置注册IP

        // 4. 保存用户
        User savedUser = userRepository.save(user);
        log.info("邮箱注册成功, userId: {}, username: {}", savedUser.getId(), savedUser.getUsername());

        return savedUser;
    }

    /**
     * 用户登录
     * 
     * @param account  账号（用户名/邮箱/手机号）
     * @param password 密码
     * @param loginIp  登录IP
     * @return 登录的用户
     */
    public User login(String account, String password, String loginIp) {
        log.info("用户登录, account: {}", account);

        // 1. 根据账号查询用户（支持用户名/邮箱/手机号）
        User user = findByAccount(account);
        if (user == null) {
            throw new IllegalArgumentException("账号或密码错误");
        }

        // 2. 验证密码
        if (!user.validatePassword(password)) {
            throw new IllegalArgumentException("账号或密码错误");
        }

        // 3. 检查用户状态
        if (!user.isActive()) {
            Integer status = user.getStatus();
            if (status != null && status == 0) {
                throw new IllegalStateException("账号已被禁用");
            } else if (status != null && status == 2) {
                throw new IllegalStateException("账号已被锁定");
            } else {
                throw new IllegalStateException("账号状态异常");
            }
        }

        // 4. 更新最后登录信息
        user.updateLastLogin(loginIp);
        userRepository.update(user);

        log.info("用户登录成功, userId: {}, username: {}", user.getId(), user.getUsername());
        return user;
    }

    /**
     * 根据账号查询用户（支持用户名/邮箱/手机号）
     * 
     * @param account 账号
     * @return 用户，不存在返回null
     */
    private User findByAccount(String account) {
        if (account == null || account.isEmpty()) {
            return null;
        }

        // 尝试按用户名查询
        Optional<User> userOpt = userRepository.findByUsername(account);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // 尝试按邮箱查询
        userOpt = userRepository.findByEmail(account);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // 尝试按手机号查询
        userOpt = userRepository.findByPhone(account);
        return userOpt.orElse(null);
    }

    /**
     * 检查用户名是否可用
     * 
     * @param username 用户名
     * @return true-可用，false-已被占用
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * 检查邮箱是否可用
     * 
     * @param email 邮箱
     * @return true-可用，false-已被占用
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * 检查手机号是否可用
     * 
     * @param phone 手机号
     * @return true-可用，false-已被占用
     */
    public boolean isPhoneAvailable(String phone) {
        return !userRepository.existsByPhone(phone);
    }
}
