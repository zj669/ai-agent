package com.zj.aiagent.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LLM 配置中心集成测试。
 */
public class LlmConfigIntegrationTest extends BaseIntegrationTest {

    @Test
    void should_ListLlmConfigs() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/llm-config", Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }
}

