package com.zj.aiagent.application.user.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserRequests {

    @Data
    public static class SendEmailCodeRequest {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
    }

    @Data
    public static class RegisterByEmailRequest {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "验证码不能为空")
        private String code;

        @NotBlank(message = "密码不能为空")
        private String password;

        private String username;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "邮箱不能为空")
        private String email;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class ModifyUserRequest {
        private String username;
        private String avatarUrl;
        private String phone;
    }
}
