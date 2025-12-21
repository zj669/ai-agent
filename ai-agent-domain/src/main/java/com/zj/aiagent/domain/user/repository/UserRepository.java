package com.zj.aiagent.domain.user.repository;

import com.zj.aiagent.domain.user.entity.User;

import java.util.Optional;

/**
 * 用户仓储接口 - 领域层
 * 
 * <p>
 * 定义用户聚合根的持久化契约，具体实现由基础设施层提供
 * 
 * @author zj
 * @since 2025-12-21
 */
public interface UserRepository {

    /**
     * 根据用户ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户对象，不存在返回Optional.empty()
     */
    Optional<User> findById(Long userId);

    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户对象，不存在返回Optional.empty()
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查询用户
     * 
     * @param email 邮箱
     * @return 用户对象，不存在返回Optional.empty()
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据手机号查询用户
     * 
     * @param phone 手机号
     * @return 用户对象，不存在返回Optional.empty()
     */
    Optional<User> findByPhone(String phone);

    /**
     * 保存用户
     * 
     * @param user 用户对象
     * @return 保存后的用户对象
     */
    User save(User user);

    /**
     * 更新用户
     * 
     * @param user 用户对象
     * @return 更新后的用户对象
     */
    User update(User user);

    /**
     * 删除用户
     * 
     * @param userId 用户ID
     */
    void deleteById(Long userId);

    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return true-存在，false-不存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     * 
     * @param email 邮箱
     * @return true-存在，false-不存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查手机号是否存在
     * 
     * @param phone 手机号
     * @return true-存在，false-不存在
     */
    boolean existsByPhone(String phone);
}
