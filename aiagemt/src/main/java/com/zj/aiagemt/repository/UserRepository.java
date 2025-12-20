package com.zj.aiagemt.repository;

import com.zj.aiagemt.model.entity.User;
import com.zj.aiagemt.repository.base.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    @Resource
    private UserMapper userMapper;

    public User selectUserById(Long userId) {
        if(userId == null){
            return null;
        }
        return userMapper.selectById(userId);
    }
}
