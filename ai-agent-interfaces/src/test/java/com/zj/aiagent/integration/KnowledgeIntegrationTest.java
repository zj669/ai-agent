package com.zj.aiagent.integration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 知识库相关集成测试（仅验证列表接口是否可用）。
 * 复杂的上传/解析依赖 MinIO 与 Milvus，此处不做强依赖。
 */
public class KnowledgeIntegrationTest extends BaseIntegrationTest {

    @Test
    void should_ListKnowledgeDatasets() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/knowledge/datasets", String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }
}

