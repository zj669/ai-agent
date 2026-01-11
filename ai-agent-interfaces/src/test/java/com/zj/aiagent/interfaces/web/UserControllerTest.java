package com.zj.aiagent.interfaces.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.user.UserApplicationService;
import com.zj.aiagent.application.user.dto.UserLoginResponse;
import com.zj.aiagent.application.user.dto.UserRequests;
import com.zj.aiagent.domain.auth.service.ITokenService;
import com.zj.aiagent.interfaces.user.web.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserApplicationService userApplicationService;

    @MockitoBean
    private ITokenService tokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testLoginSuccess() throws Exception {
        UserRequests.LoginRequest request = new UserRequests.LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("123456");

        String token = "mock-token";
        UserLoginResponse resp = new UserLoginResponse();
        resp.setToken(token);

        when(userApplicationService.login(any(), any())).thenReturn(resp);

        mockMvc.perform(post("/client/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value(token));
    }
}
