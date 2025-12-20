package com.zj.aiagemt.controller.client;

import com.zj.aiagemt.model.common.Response;
import com.zj.aiagemt.model.dto.EmailRegisterDTO;
import com.zj.aiagemt.model.dto.LoginDTO;
import com.zj.aiagemt.model.dto.RegisterDTO;
import com.zj.aiagemt.model.dto.SendEmailCodeDTO;
import com.zj.aiagemt.model.entity.User;
import com.zj.aiagemt.model.vo.UserVO;
import com.zj.aiagemt.service.EmailLimitService;
import com.zj.aiagemt.service.EmailService;
import com.zj.aiagemt.service.UserService;
import com.zj.aiagemt.utils.IpUtils;
import com.zj.aiagemt.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/client/user")
@Tag(name = "用户管理", description = "用户注册、登录等接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.OPTIONS })
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private EmailService emailService;

    @Resource
    private EmailLimitService emailLimitService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Response<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        log.info("用户注册请求, username: {}", dto.getUsername());
        try {
            UserVO userVO = userService.register(dto);
            return Response.success(userVO);
        } catch (IllegalArgumentException e) {
            log.warn("用户注册失败: {}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户注册异常", e);
            return Response.fail("注册失败,请稍后重试");
        }
    }

    @PostMapping("/email/sendCode")
    @Operation(summary = "发送邮箱验证码")
    public Response<Void> sendEmailCode(@Valid @RequestBody SendEmailCodeDTO dto, HttpServletRequest request) {
        log.info("发送邮箱验证码请求, email: {}", dto.getEmail());
        try {
            // 获取真实IP
            String ip = IpUtils.getRealIp(request);

            // 执行三重限流检查
            emailLimitService.checkAllLimits(dto.getEmail(), ip, dto.getDeviceId());

            // 发送验证码
            emailService.sendVerificationCode(dto.getEmail(), ip, dto.getDeviceId());

            return Response.success();
        } catch (IllegalStateException e) {
            log.warn("发送验证码失败(限流): {}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("发送验证码异常", e);
            return Response.fail("发送失败,请稍后重试");
        }
    }

    @PostMapping("/email/register")
    @Operation(summary = "邮箱注册")
    public Response<UserVO> emailRegister(@Valid @RequestBody EmailRegisterDTO dto, HttpServletRequest request) {
        log.info("邮箱注册请求, email: {}", dto.getEmail());
        try {
            // 获取真实IP
            String ip = IpUtils.getRealIp(request);

            // 执行注册
            UserVO userVO = userService.emailRegister(dto, ip);
            return Response.success(userVO);
        } catch (IllegalArgumentException e) {
            log.warn("邮箱注册失败: {}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("邮箱注册异常", e);
            return Response.fail("注册失败,请稍后重试");
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Response<UserVO> login(@Valid @RequestBody LoginDTO dto) {
        log.info("用户登录请求, account: {}", dto.getAccount());
        try {
            UserVO userVO = userService.login(dto);
            return Response.success(userVO);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("用户登录失败: {}", e.getMessage());
            return Response.fail(e.getMessage());
        } catch (Exception e) {
            log.error("用户登录异常", e);
            return Response.fail("登录失败,请稍后重试");
        }
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前登录用户信息")
    public Response<UserVO> getUserInfo() {
        try {
            Long userId = UserContext.getUserId();
            if (userId == null) {
                return Response.fail("401", "未登录");
            }

            User user = userService.getUserById(userId);
            if (user == null) {
                return Response.fail("用户不存在");
            }

            UserVO userVO = UserVO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .build();

            return Response.success(userVO);
        } catch (Exception e) {
            log.error("获取用户信息异常", e);
            return Response.fail("获取用户信息失败");
        }
    }
}
