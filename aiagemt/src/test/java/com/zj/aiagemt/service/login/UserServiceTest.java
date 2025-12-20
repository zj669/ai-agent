package com.zj.aiagemt.service.login;

import com.zj.aiagemt.model.dto.LoginDTO;
import com.zj.aiagemt.model.dto.RegisterDTO;
import com.zj.aiagemt.model.entity.User;
import com.zj.aiagemt.model.vo.UserVO;
import com.zj.aiagemt.repository.base.UserMapper;
import com.zj.aiagemt.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试
 */
@Slf4j
@SpringBootTest
@Transactional // 测试后回滚
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Test
    void testRegister_Success() {
        log.info("测试用户注册成功场景");

        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("testuser" + System.currentTimeMillis());
        dto.setPassword("123456");
        dto.setEmail("test@example.com");
        dto.setPhone("13800138000");

        UserVO userVO = userService.register(dto);

        assertNotNull(userVO);
        assertNotNull(userVO.getId());
        assertEquals(dto.getUsername(), userVO.getUsername());
        assertEquals(dto.getEmail(), userVO.getEmail());
        assertEquals(dto.getPhone(), userVO.getPhone());
        assertNotNull(userVO.getToken());

        log.info("用户注册成功测试通过, userId: {}", userVO.getId());
    }

    @Test
    void testRegister_DuplicateUsername() {
        log.info("测试注册重复用户名场景");

        String username = "duplicate_user_" + System.currentTimeMillis();

        // 第一次注册
        RegisterDTO dto1 = new RegisterDTO();
        dto1.setUsername(username);
        dto1.setPassword("123456");
        userService.register(dto1);

        // 第二次注册相同用户名
        RegisterDTO dto2 = new RegisterDTO();
        dto2.setUsername(username);
        dto2.setPassword("654321");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(dto2));

        assertEquals("用户名已存在", exception.getMessage());
        log.info("重复用户名测试通过");
    }

    @Test
    void testLogin_Success() {
        log.info("测试用户登录成功场景");

        // 先注册一个用户
        String username = "logintest_" + System.currentTimeMillis();
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(username);
        registerDTO.setPassword("123456");
        registerDTO.setEmail("login@test.com");
        userService.register(registerDTO);

        // 使用用户名登录
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccount(username);
        loginDTO.setPassword("123456");

        UserVO userVO = userService.login(loginDTO);

        assertNotNull(userVO);
        assertEquals(username, userVO.getUsername());
        assertNotNull(userVO.getToken());

        log.info("用户登录成功测试通过, userId: {}", userVO.getId());
    }

    @Test
    void testLogin_WithEmail() {
        log.info("测试使用邮箱登录场景");

        // 先注册一个用户
        String email = "emailtest_" + System.currentTimeMillis() + "@test.com";
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("emailuser_" + System.currentTimeMillis());
        registerDTO.setPassword("123456");
        registerDTO.setEmail(email);
        userService.register(registerDTO);

        // 使用邮箱登录
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccount(email);
        loginDTO.setPassword("123456");

        UserVO userVO = userService.login(loginDTO);

        assertNotNull(userVO);
        assertEquals(email, userVO.getEmail());
        assertNotNull(userVO.getToken());

        log.info("使用邮箱登录测试通过, userId: {}", userVO.getId());
    }

    @Test
    void testLogin_WrongPassword() {
        log.info("测试密码错误场景");

        // 先注册一个用户
        String username = "wrongpwd_" + System.currentTimeMillis();
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(username);
        registerDTO.setPassword("123456");
        userService.register(registerDTO);

        // 使用错误密码登录
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccount(username);
        loginDTO.setPassword("wrongpassword");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(loginDTO));

        assertEquals("账号或密码错误", exception.getMessage());
        log.info("密码错误测试通过");
    }

    @Test
    void testLogin_AccountNotFound() {
        log.info("测试账号不存在场景");

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setAccount("nonexistent_user");
        loginDTO.setPassword("123456");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.login(loginDTO));

        assertEquals("账号或密码错误", exception.getMessage());
        log.info("账号不存在测试通过");
    }

    @Test
    void testGetUserById() {
        log.info("测试根据ID查询用户");

        // 先注册一个用户
        String username = "getbyid_" + System.currentTimeMillis();
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(username);
        registerDTO.setPassword("123456");
        UserVO registeredUser = userService.register(registerDTO);

        // 根据ID查询
        User user = userService.getUserById(registeredUser.getId());

        assertNotNull(user);
        assertEquals(registeredUser.getId(), user.getId());
        assertEquals(username, user.getUsername());

        log.info("根据ID查询用户测试通过");
    }
}
