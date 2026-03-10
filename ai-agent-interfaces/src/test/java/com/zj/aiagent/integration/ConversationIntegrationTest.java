package com.zj.aiagent.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 对话管理相关集成测试（最小链路）。
 */
public class ConversationIntegrationTest extends BaseIntegrationTest {

    @Test
    void should_ListConversations() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/chat/conversations", Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }
}

