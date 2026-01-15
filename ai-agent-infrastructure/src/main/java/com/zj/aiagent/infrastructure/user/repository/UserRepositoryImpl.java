package com.zj.aiagent.infrastructure.user.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.repository.IUserRepository;
import com.zj.aiagent.domain.user.valobj.Email;
import com.zj.aiagent.infrastructure.user.mapper.EmailLogMapper;
import com.zj.aiagent.infrastructure.user.mapper.UserMapper;
import com.zj.aiagent.infrastructure.user.po.UserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements IUserRepository {

    private final UserMapper userMapper;
    private final EmailLogMapper emailLogMapper;

    @Override
    public User save(User user) {
        UserPO po = toPO(user);
        if (po.getId() == null) {
            userMapper.insert(po);
        } else {
            userMapper.updateById(po);
        }
        return toEntity(userMapper.selectById(po.getId()));
    }

    @Override
    public User findById(Long id) {
        UserPO po = userMapper.selectById(id);
        return toEntity(po);
    }

    @Override
    public User findByEmail(Email email) {
        UserPO po = userMapper.selectOne(new LambdaQueryWrapper<UserPO>()
                .eq(UserPO::getEmail, email.getValue()));
        return toEntity(po);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return userMapper.exists(new LambdaQueryWrapper<UserPO>()
                .eq(UserPO::getEmail, email.getValue()));
    }

    @Override
    public void saveEmailLog(Email email, String code) {

    }

    private UserPO toPO(User user) {
        if (user == null)
            return null;
        UserPO po = new UserPO();
        po.setId(user.getId());
        po.setUsername(user.getUsername());
        if (user.getEmail() != null) {
            po.setEmail(user.getEmail().getValue());
        }
        if (user.getCredential() != null) {
            po.setPassword(user.getCredential().getEncryptedPassword());
        }
        po.setPhone(user.getPhone());
        po.setAvatarUrl(user.getAvatarUrl());
        if (user.getStatus() != null) {
            po.setStatus(user.getStatus().getCode());
        }
        po.setLastLoginIp(user.getLastLoginIp());
        po.setLastLoginTime(user.getLastLoginTime());
        po.setDeleted(user.getDeleted());
        po.setCreatedAt(user.getCreatedAt());
        po.setUpdatedAt(user.getUpdatedAt());
        return po;
    }

    private User toEntity(UserPO po) {
        if (po == null)
            return null;
        return User.reconstruct(
                po.getId(),
                po.getUsername(),
                po.getEmail(),
                po.getPassword(),
                po.getPhone(),
                po.getAvatarUrl(),
                po.getStatus(),
                po.getLastLoginIp(),
                po.getLastLoginTime(),
                po.getDeleted(),
                po.getCreatedAt(),
                po.getUpdatedAt());
    }
}
