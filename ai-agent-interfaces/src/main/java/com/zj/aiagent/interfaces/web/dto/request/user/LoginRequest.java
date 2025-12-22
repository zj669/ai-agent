package com.zj.aiagent.interfaces.web.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求
 * 
 * @author zj
 * @since 2025-12-21
 */
@Data
@Schema(description = "用户登录请求")
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    @Schema(description = "账号(用户名/邮箱/手机号)", example = "testuser")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;
}
