package com.zj.aiagemt.service;

import com.zj.aiagemt.model.dto.LoginDTO;
import com.zj.aiagemt.model.dto.RegisterDTO;
import com.zj.aiagemt.model.entity.User;
import com.zj.aiagemt.model.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param dto 注册信息
     * @return 用户信息(包含Token)
     */
    UserVO register(RegisterDTO dto);

    /**
     * 用户登录
     *
     * @param dto 登录信息
     * @return 用户信息(包含Token)
     */
    UserVO login(LoginDTO dto);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户实体
     */
    User getUserById(Long id);
}
