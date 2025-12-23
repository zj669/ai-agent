package com.zj.aiagent.interfaces.web.controller.client;

import com.zj.aiagent.application.user.UserApplicationService;
import com.zj.aiagent.application.user.command.LoginCommand;
import com.zj.aiagent.application.user.command.RegisterByEmailCommand;
import com.zj.aiagent.application.user.command.SendEmailCodeCommand;
import com.zj.aiagent.application.user.query.GetUserInfoQuery;
import com.zj.aiagent.interfaces.common.Response;
import com.zj.aiagent.interfaces.web.dto.request.user.LoginRequest;
import com.zj.aiagent.interfaces.web.dto.request.user.RegisterByEmailRequest;
import com.zj.aiagent.interfaces.web.dto.request.user.SendEmailCodeRequest;
import com.zj.aiagent.interfaces.web.dto.response.user.UserResponse;
import com.zj.aiagent.shared.utils.IpUtils;
import com.zj.aiagent.shared.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@RestController
@RequestMapping("/client/user")
@Tag(name = "用户管理", description = "用户注册、登录等接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.OPTIONS
})
public class UserController {

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/email/sendCode")
    @Operation(summary = "发送邮箱验证码")
    public Response<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeRequest request,
            HttpServletRequest httpRequest) {
        log.info("发送邮箱验证码请求, email: {}", request.getEmail());
        try {
            // 获取真实IP
            String ip = IpUtils.getRealIp(httpRequest);

            // 构建命令
            SendEmailCodeCommand command = SendEmailCodeCommand.builder()
                    .email(request.getEmail())
                    .ip(ip)
                    .deviceId(request.getDeviceId())
                    .build();

            // 执行发送
            userApplicationService.sendEmailCode(command);

            return Response.success();
        } catch (IllegalStateException e) {
            log.warn("发送验证码失败(限流): {}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("发送验证码异常", e);
            return Response.fail("发送失败,请稍后重试");
        }
    }

    /**
     * 邮箱注册
     */
    @PostMapping("/email/register")
    @Operation(summary = "邮箱注册")
    public Response<UserResponse> emailRegister(@Valid @RequestBody RegisterByEmailRequest request,
            HttpServletRequest httpRequest) {
        log.info("邮箱注册请求, email: {}", request.getEmail());
        try {
            // 获取真实IP
            String ip = IpUtils.getRealIp(httpRequest);

            // 构建命令
            RegisterByEmailCommand command = RegisterByEmailCommand.builder()
                    .email(request.getEmail())
                    .code(request.getCode())
                    .password(request.getPassword())
                    .username(request.getUsername())
                    .deviceId(request.getDeviceId())
                    .ip(ip)
                    .build();

            // 执行注册
            UserApplicationService.UserDTO userDTO = userApplicationService.registerByEmail(command);

            // 转换响应
            UserResponse response = convertToResponse(userDTO);
            return Response.success(response);
        } catch (IllegalArgumentException e) {
            log.warn("邮箱注册失败: {}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("邮箱注册异常", e);
            return Response.fail("注册失败,请稍后重试");
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Response<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("用户登录请求, account: {}", request.getEmail());
        try {
            // 获取真实IP
            String ip = IpUtils.getRealIp(httpRequest);

            // 构建命令
            LoginCommand command = LoginCommand.builder()
                    .account(request.getEmail())
                    .password(request.getPassword())
                    .loginIp(ip)
                    .build();

            // 执行登录
            UserApplicationService.UserDTO userDTO = userApplicationService.login(command);

            // 转换响应
            UserResponse response = convertToResponse(userDTO);
            return Response.success(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("用户登录失败: {}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户登录异常", e);
            return Response.fail("登录失败,请稍后重试");
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前登录用户信息")
    public Response<UserResponse> getUserInfo() {
        try {
            Long userId = UserContext.getUserId();
            if (userId == null) {
                return Response.fail("401", "未登录");
            }

            // 构建查询
            GetUserInfoQuery query = GetUserInfoQuery.builder()
                    .userId(userId)
                    .build();

            // 执行查询
            UserApplicationService.UserDTO userDTO = userApplicationService.getUserInfo(query);

            // 转换响应
            UserResponse response = convertToResponse(userDTO);
            return Response.success(response);
        } catch (IllegalArgumentException e) {
            log.warn("获取用户信息失败: {}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取用户信息异常", e);
            return Response.fail("获取用户信息失败");
        }
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Response<Void> logout(HttpServletRequest request) {
        try {
            // 从请求头获取Token
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            if (token == null || token.isEmpty()) {
                return Response.fail("Token不能为空");
            }

            // 执行退出登录
            userApplicationService.logout(token);

            return Response.success();
        } catch (Exception e) {
            log.error("退出登录异常", e);
            return Response.fail("退出登录失败");
        }
    }

    /**
     * 转换DTO到Response
     */
    private UserResponse convertToResponse(UserApplicationService.UserDTO userDTO) {
        return UserResponse.builder()
                .id(userDTO.getId())
                .username(userDTO.getUsername())
                .email(userDTO.getEmail())
                .phone(userDTO.getPhone())
                .token(userDTO.getToken())
                .build();
    }
}
