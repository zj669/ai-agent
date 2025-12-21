package com.zj.aiagent.domain.user.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


@Data
public class User {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码哈希
     */
    private String passwordHash;

    /**
     * 盐值
     */
    private String salt;

    /**
     * 状态：0-禁用，1-正常
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;
    private LocalDateTime createTime;

    private Long createBy;

    private LocalDateTime updateTime;

    private Long updateBy;

    /**
     * 检查用户是否激活
     * 
     * @return true-已激活，false-已禁用
     */
    public boolean isActive() {
        return status != null && status == 1;
    }
}
