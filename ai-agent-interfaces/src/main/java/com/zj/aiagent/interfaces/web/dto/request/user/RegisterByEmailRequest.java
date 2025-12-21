package com.zj.aiagent.interfaces.web.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 邮箱注册请求
 * 
 * @author zj
 * @since 2025-12-21
 */
@Data
@Schema(description = "邮箱注册请求")
public class RegisterByEmailRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "test@example.com")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须是6位数字")
    @Schema(description = "验证码", example = "123456")
    private String code;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    @Schema(description = "密码", example = "password123")
    private String password;

    @Size(max = 20, message = "用户名长度不能超过20个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]*$", message = "用户名只能包含字母、数字和下划线")
    @Schema(description = "用户名(可选,不填则根据邮箱自动生成)", example = "testuser")
    private String username;

    @Schema(description = "设备指纹", example = "device-fingerprint-12345")
    private String deviceId;
}
