package com.zj.aiagemt.controller;

import com.zj.aiagemt.model.common.Response;
import com.zj.aiagemt.model.dto.LoginDTO;
import com.zj.aiagemt.model.dto.RegisterDTO;
import com.zj.aiagemt.model.entity.User;
import com.zj.aiagemt.model.vo.UserVO;
import com.zj.aiagemt.service.UserService;
import com.zj.aiagemt.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "用户管理", description = "用户注册、登录等接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST,
        RequestMethod.OPTIONS })
public class UserController {

    @Resource
    private UserService userService;

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
