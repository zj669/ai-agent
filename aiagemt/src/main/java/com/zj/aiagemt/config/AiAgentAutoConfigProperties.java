package com.zj.aiagemt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;


@Data
@ConfigurationProperties(prefix = "spring.ai.agent.auto-config")
public class AiAgentAutoConfigProperties {

    /**
     * 是否启用AI Agent自动装配
     */
    private boolean enabled = false;
}
