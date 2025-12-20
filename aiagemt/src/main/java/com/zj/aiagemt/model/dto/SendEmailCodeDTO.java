package com.zj.aiagemt.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送邮箱验证码DTO
 */
@Data
@Schema(description = "发送邮箱验证码请求")
public class SendEmailCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱地址", example = "test@example.com")
    private String email;

    @Schema(description = "设备指纹", example = "device-fingerprint-12345")
    private String deviceId;
}
