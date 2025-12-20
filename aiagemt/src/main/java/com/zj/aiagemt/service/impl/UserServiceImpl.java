package com.zj.aiagemt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagemt.model.dto.EmailRegisterDTO;
import com.zj.aiagemt.model.dto.LoginDTO;
import com.zj.aiagemt.model.dto.RegisterDTO;
import com.zj.aiagemt.model.entity.User;
import com.zj.aiagemt.model.vo.UserVO;
import com.zj.aiagemt.repository.base.UserMapper;
import com.zj.aiagemt.service.EmailService;
import com.zj.aiagemt.service.UserService;
import com.zj.aiagemt.utils.JwtUtil;
import com.zj.aiagemt.utils.PasswordUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private EmailService emailService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterDTO dto) {
        log.info("用户注册请求, username: {}", dto.getUsername());

        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> usernameQuery = new LambdaQueryWrapper<>();
        usernameQuery.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(usernameQuery) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 2. 检查邮箱是否已存在
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
            emailQuery.eq(User::getEmail, dto.getEmail());
            if (userMapper.selectCount(emailQuery) > 0) {
                throw new IllegalArgumentException("邮箱已被注册");
            }
        }

        // 3. 检查手机号是否已存在
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            LambdaQueryWrapper<User> phoneQuery = new LambdaQueryWrapper<>();
            phoneQuery.eq(User::getPhone, dto.getPhone());
            if (userMapper.selectCount(phoneQuery) > 0) {
                throw new IllegalArgumentException("手机号已被注册");
            }
        }

        // 4. 生成盐值和密码哈希
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(dto.getPassword(), salt);

        // 5. 创建用户实体
        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .passwordHash(passwordHash)
                .salt(salt)
                .status(1) // 默认启用
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 6. 保存用户
        userMapper.insert(user);
        log.info("用户注册成功, userId: {}, username: {}", user.getId(), user.getUsername());

        // 7. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId());

        // 8. 返回用户信息
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .token(token)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO emailRegister(EmailRegisterDTO dto, String ip) {
        log.info("邮箱注册请求, email: {}", dto.getEmail());

        // 1. 验证验证码
        if (!emailService.verifyCode(dto.getEmail(), dto.getCode())) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }

        // 2. 检查邮箱是否已被注册
        LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
        emailQuery.eq(User::getEmail, dto.getEmail());
        if (userMapper.selectCount(emailQuery) > 0) {
            throw new IllegalArgumentException("该邮箱已被注册");
        }

        // 3. 生成或验证用户名
        String username = dto.getUsername();
        if (username == null || username.isEmpty()) {
            // 根据邮箱自动生成用户名
            username = generateUsernameFromEmail(dto.getEmail());
        } else {
            // 检查用户名是否已存在
            LambdaQueryWrapper<User> usernameQuery = new LambdaQueryWrapper<>();
            usernameQuery.eq(User::getUsername, username);
            if (userMapper.selectCount(usernameQuery) > 0) {
                throw new IllegalArgumentException("用户名已存在");
            }
        }

        // 4. 生成盐值和密码哈希
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(dto.getPassword(), salt);

        // 5. 创建用户实体
        User user = User.builder()
                .username(username)
                .email(dto.getEmail())
                .passwordHash(passwordHash)
                .salt(salt)
                .status(1) // 默认启用
                .lastLoginIp(ip)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 6. 保存用户
        userMapper.insert(user);
        log.info("邮箱注册成功, userId: {}, username: {}, email: {}",
                user.getId(), user.getUsername(), user.getEmail());

        // 7. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId());

        // 8. 返回用户信息
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .build();
    }

    /**
     * 根据邮箱生成唯一用户名
     * 
     * @param email 邮箱地址
     * @return 用户名
     */
    private String generateUsernameFromEmail(String email) {
        // 从邮箱中提取前缀
        String prefix = email.split("@")[0];
        // 只保留字母和数字
        prefix = prefix.replaceAll("[^a-zA-Z0-9]", "");
        // 限制长度
        if (prefix.length() > 15) {
            prefix = prefix.substring(0, 15);
        }

        // 尝试生成唯一用户名
        String username = prefix;
        int attempts = 0;
        Random random = new Random();

        while (attempts < 100) {
            LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
            query.eq(User::getUsername, username);
            if (userMapper.selectCount(query) == 0) {
                return username;
            }

            // 添加随机后缀
            username = prefix + "_" + (1000 + random.nextInt(9000));
            attempts++;
        }

        // 如果100次都失败,使用时间戳
        return prefix + "_" + System.currentTimeMillis();
    }

    @Override
    public UserVO login(LoginDTO dto) {
        log.info("用户登录请求, account: {}", dto.getAccount());

        // 1. 根据账号(用户名/邮箱/手机号)查询用户
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.and(wrapper -> wrapper
                .eq(User::getUsername, dto.getAccount())
                .or()
                .eq(User::getEmail, dto.getAccount())
                .or()
                .eq(User::getPhone, dto.getAccount()));

        User user = userMapper.selectOne(query);
        if (user == null) {
            throw new IllegalArgumentException("账号或密码错误");
        }

        // 2. 验证密码
        if (!PasswordUtil.verifyPassword(dto.getPassword(), user.getSalt(), user.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            throw new IllegalStateException("账号已被禁用");
        } else if (user.getStatus() == 2) {
            throw new IllegalStateException("账号已被锁定");
        }

        // 4. 更新最后登录时间和IP(这里暂时不记录IP,可以后续从Request中获取)
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功, userId: {}, username: {}", user.getId(), user.getUsername());

        // 5. 生成JWT Token
        String token = jwtUtil.generateToken(user.getId());

        // 6. 返回用户信息
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .token(token)
                .build();
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
}
