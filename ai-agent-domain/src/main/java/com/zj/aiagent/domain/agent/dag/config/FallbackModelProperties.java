package com.zj.aiagent.domain.agent.dag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 降级模型配置 - 从配置文件读取
 * 当主模型调用失败时，使用此配置的模型进行降级处理
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.fallback")
public class FallbackModelProperties {

    /** 是否启用降级模型 */
    private Boolean enabled = true;

    /** API 基础 URL */
    private String baseUrl = "https://api.openai.com/v1";

    /** API Key */
    private String apiKey;

    /** 模型名称，默认使用成本较低的模型 */
    private String modelName = "gpt-3.5-turbo";

    /** 温度参数 */
    private Double temperature = 0.7;

    /** 最大 token 数 */
    private Integer maxTokens = 2000;

    /** Top P 参数 */
    private Double topP = 1.0;

    /**
     * 检查降级模型是否可用
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(enabled)
                && baseUrl != null && !baseUrl.isEmpty()
                && apiKey != null && !apiKey.isEmpty();
    }
}
