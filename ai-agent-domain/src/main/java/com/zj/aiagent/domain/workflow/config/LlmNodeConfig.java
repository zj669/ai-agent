package com.zj.aiagent.domain.workflow.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * LLM 节点配置
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LlmNodeConfig extends NodeConfig {

    /**
     * 模型名称（如 gpt-4, claude-3）
     */
    private String model;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 用户提示词模板（支持 SpEL）
     */
    private String promptTemplate;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 最大 Token 数
     */
    private Integer maxTokens;

    /**
     * 是否流式输出
     */
    private boolean stream;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
}
