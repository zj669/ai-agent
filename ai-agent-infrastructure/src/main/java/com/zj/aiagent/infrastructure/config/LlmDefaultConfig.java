package com.zj.aiagent.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 默认配置
 * 当工作流节点未配置模型参数时，回退到此默认值
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm.default")
public class LlmDefaultConfig {

    private String baseUrl;
    private String model;
    private String apiKey;
}
