package com.zj.aiagent.infrastructure.user.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.repository.UserRepository;
import com.zj.aiagent.infrastructure.user.converter.UserConverter;
import com.zj.aiagent.infrastructure.persistence.mapper.UserMapper;
import com.zj.aiagent.infrastructure.persistence.entity.UserPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储实现 - 基础设施层
 * 
 * <p>
 * 实现领域层定义的UserRepository接口，使用MyBatis Plus进行持久化
 * 
 * @author zj
 * @since 2025-12-21
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    @Resource
    private UserMapper userMapper;

    @Override
    public Optional<User> findById(Long userId) {
        UserPO po = userMapper.selectById(userId);
        return Optional.ofNullable(UserConverter.toDomain(po));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getUsername, username);
        UserPO po = userMapper.selectOne(wrapper);
        return Optional.ofNullable(UserConverter.toDomain(po));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getEmail, email);
        UserPO po = userMapper.selectOne(wrapper);
        return Optional.ofNullable(UserConverter.toDomain(po));
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getPhone, phone);
        UserPO po = userMapper.selectOne(wrapper);
        return Optional.ofNullable(UserConverter.toDomain(po));
    }

    @Override
    public User save(User user) {
        UserPO po = UserConverter.toPO(user);
        userMapper.insert(po);
        user.setId(po.getId());
        return user;
    }

    @Override
    public User update(User user) {
        UserPO po = UserConverter.toPO(user);
        userMapper.updateById(po);
        return user;
    }

    @Override
    public void deleteById(Long userId) {
        userMapper.deleteById(userId);
    }

    @Override
    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getUsername, username);
        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getEmail, email);
        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getPhone, phone);
        return userMapper.selectCount(wrapper) > 0;
    }
}
