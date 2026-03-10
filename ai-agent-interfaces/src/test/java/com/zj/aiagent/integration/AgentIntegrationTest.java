package com.zj.aiagent.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Agent 管理相关集成测试（最小 CRUD 验证）。
 */
public class AgentIntegrationTest extends BaseIntegrationTest {

    @Test
    void should_ListAgents() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/agent", Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }
}

