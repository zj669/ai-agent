package com.zj.aiagent.infrastructure.user.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;

public class EmailLogPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String password;
    private String phone;
    private String avatarUrl;
    private Integer status;
    private String lastLoginIp;
    private LocalDateTime lastLoginTime;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
