package com.zj.aiagent.domain.agent.dag.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Advisor配置类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorConfig {

    /**
     * Advisor ID
     */
    private String advisorId;

    /**
     * Advisor类型 (MEMORY, RAG, TOOL, CUSTOM)
     */
    private String advisorType;

    /**
     * Advisor配置参数
     */
    private Map<String, Object> config;
}
