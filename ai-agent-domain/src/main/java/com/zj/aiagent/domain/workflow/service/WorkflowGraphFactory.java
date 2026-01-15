package com.zj.aiagent.domain.workflow.service;

import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;

/**
 * 工作流图工厂接口（领域服务端口）
 * 负责将 graphJson 字符串解析为 WorkflowGraph 领域对象
 */
public interface WorkflowGraphFactory {

    /**
     * 从 JSON 字符串解析工作流图
     *
     * @param graphJson 工作流图的 JSON 表示
     * @return 解析后的 WorkflowGraph 领域对象
     * @throws IllegalArgumentException 如果 JSON 格式无效
     */
    WorkflowGraph fromJson(String graphJson);
}
