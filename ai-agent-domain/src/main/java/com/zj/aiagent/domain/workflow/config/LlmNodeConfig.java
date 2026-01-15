package com.zj.aiagent.domain.workflow.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
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

    // ========== 环境感知配置 ==========

    /**
     * 是否注入全局执行日志
     * 让 LLM 知道当前工作流执行进度
     */
    @Builder.Default
    private boolean includeExecutionLog = true;

    /**
     * 显式引用的前序节点ID列表
     * 用于深度获取特定节点的完整输出，注入到 System Prompt
     */
    @Builder.Default
    private List<String> contextRefNodes = new ArrayList<>();

    /**
     * 是否注入会话历史 (STM)
     * 默认开启
     */
    @Builder.Default
    private boolean includeChatHistory = true;

    /**
     * 会话历史最大轮数
     * 控制注入到 Prompt 的历史消息数量
     */
    @Builder.Default
    private int maxHistoryRounds = 10;
}
