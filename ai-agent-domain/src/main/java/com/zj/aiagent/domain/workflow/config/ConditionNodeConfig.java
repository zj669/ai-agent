package com.zj.aiagent.domain.workflow.config;

import com.zj.aiagent.domain.workflow.valobj.Branch;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 条件路由节点配置
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConditionNodeConfig extends NodeConfig {

    /**
     * 路由策略
     */
    public enum RoutingStrategy {
        /**
         * 表达式路由（SpEL）
         */
        EXPRESSION,

        /**
         * LLM 语义路由
         */
        LLM
    }

    /**
     * 路由策略类型
     */
    private RoutingStrategy routingStrategy;

    /**
     * 分支列表
     */
    private List<Branch> branches;

    /**
     * LLM 配置（仅 LLM 模式使用）
     */
    private LlmNodeConfig llmConfig;

    /**
     * 默认分支ID
     */
    private String defaultBranchId;
}
