package com.zj.aiagent.interfaces.user.web;

import com.zj.aiagent.application.user.UserApplicationService;
import com.zj.aiagent.application.user.dto.UserDetailDTO;
import com.zj.aiagent.application.user.dto.UserLoginResponse;
import com.zj.aiagent.application.user.dto.UserRequests;
import com.zj.aiagent.domain.auth.service.ITokenService;
import com.zj.aiagent.shared.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client/user")
@Tag(name = "User Management", description = "用户注册、登录、信息管理")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;
    private final ITokenService tokenService;

    @PostMapping("/email/sendCode")
    @Operation(summary = "发送邮箱验证码")
    public Response<Void> sendEmailCode(@Valid @RequestBody UserRequests.SendEmailCodeRequest request) {
        userApplicationService.sendEmailCode(request);
        return Response.success();
    }

    @PostMapping("/email/register")
    @Operation(summary = "邮箱注册")
    public Response<UserLoginResponse> registerByEmail(
            @Valid @RequestBody UserRequests.RegisterByEmailRequest request) {
        UserLoginResponse response = userApplicationService.registerByEmail(request);
        return Response.success(response);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Response<UserLoginResponse> login(
            @Valid @RequestBody UserRequests.LoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        UserLoginResponse response = userApplicationService.login(request, ip);
        return Response.success(response);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Response<UserDetailDTO> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = resolveUserId(token);
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        UserDetailDTO info = userApplicationService.getUserInfo(userId);
        return Response.success(info);
    }

    @PostMapping("/modify")
    @Operation(summary = "修改用户信息")
    public Response<UserDetailDTO> modifyUserInfo(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody UserRequests.ModifyUserRequest request) {
        Long userId = resolveUserId(token);
        if (userId == null) {
            return Response.error(401, "Unauthorized");
        }
        UserDetailDTO info = userApplicationService.modifyInfo(userId, request);
        return Response.success(info);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Response<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        userApplicationService.logout(token);
        return Response.success();
    }

    private Long resolveUserId(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || !tokenService.validateToken(token)) {
            return null;
        }
        return tokenService.getUserIdFromToken(token);
    }

    /**
     * 获取客户端真实IP（支持代理场景）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
