package com.zj.aiagent.integration;

import com.zj.aiagent.interfaces.user.dto.UserRequests;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户认证相关集成测试。
 * 依赖本地 Docker MySQL，使用 local profile。
 */
public class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void should_LoginAndGetUserInfo() {
        // 1) 使用已存在的测试账号登录（假设初始化脚本中已插入 admin 用户）
        Map<String, String> loginBody = Map.of(
                "email", "admin@example.com",
                "password", "admin123"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> loginResp = restTemplate.postForEntity(
                "/client/user/login",
                new HttpEntity<>(loginBody, headers),
                Map.class);

        assertThat(loginResp.getStatusCode().is2xxSuccessful()).isTrue();
        Map<?, ?> data = (Map<?, ?>) loginResp.getBody().get("data");
        assertThat(data).isNotNull();
        String token = (String) data.get("token");
        assertThat(token).isNotBlank();

        // 2) 携带 token 调用 /client/user/info
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.set("Authorization", "Bearer " + token);
        ResponseEntity<Map> infoResp = restTemplate.exchange(
                "/client/user/info",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders),
                Map.class);

        assertThat(infoResp.getStatusCode().is2xxSuccessful()).isTrue();
        Map<?, ?> infoData = (Map<?, ?>) infoResp.getBody().get("data");
        assertThat(infoData).isNotNull();
    }
}

