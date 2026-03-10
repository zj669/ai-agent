package com.zj.aiagent.integration;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 集成测试基类。
 * 依赖本地 Docker 环境（MySQL/Redis/Milvus 等），使用 local profile。
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
public abstract class BaseIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;
}

