package com.zj.aiagent.infrastructure.user.converter;

import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.infrastructure.persistence.entity.UserPO;

/**
 * 用户转换器 - 基础设施层
 * 
 * <p>
 * 负责领域对象User和持久化对象UserPO之间的转换
 * 
 * @author zj
 * @since 2025-12-21
 */
public class UserConverter {

    /**
     * PO转换为领域对象
     * 
     * @param po 持久化对象
     * @return 领域对象
     */
    public static User toDomain(UserPO po) {
        if (po == null) {
            return null;
        }

        User user = new User();
        user.setId(po.getId());
        user.setUsername(po.getUsername());
        user.setEmail(po.getEmail());
        user.setPhone(po.getPhone());
        user.setPasswordHash(po.getPasswordHash());
        user.setSalt(po.getSalt());
        user.setStatus(po.getStatus());
        user.setLastLoginTime(po.getLastLoginTime());
        user.setLastLoginIp(po.getLastLoginIp());
        user.setCreateTime(po.getCreateTime());
        user.setUpdateTime(po.getUpdateTime());

        return user;
    }

    /**
     * 领域对象转换为PO
     * 
     * @param user 领域对象
     * @return 持久化对象
     */
    public static UserPO toPO(User user) {
        if (user == null) {
            return null;
        }

        UserPO po = new UserPO();
        po.setId(user.getId());
        po.setUsername(user.getUsername());
        po.setEmail(user.getEmail());
        po.setPhone(user.getPhone());
        po.setPasswordHash(user.getPasswordHash());
        po.setSalt(user.getSalt());
        po.setStatus(user.getStatus());
        po.setLastLoginTime(user.getLastLoginTime());
        po.setLastLoginIp(user.getLastLoginIp());
        po.setCreateTime(user.getCreateTime());
        po.setUpdateTime(user.getUpdateTime());

        return po;
    }
}
