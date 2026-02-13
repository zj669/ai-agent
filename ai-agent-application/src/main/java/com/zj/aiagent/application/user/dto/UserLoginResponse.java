package com.zj.aiagent.application.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponse {
    private String token;
    private String refreshToken; // 新增
    private Long expireIn; // seconds
    private String deviceId; // 新增
    private UserDetailDTO user;
}
