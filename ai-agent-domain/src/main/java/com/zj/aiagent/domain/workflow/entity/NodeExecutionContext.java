package com.zj.aiagent.domain.workflow.entity;

import com.alibaba.fastjson2.JSONObject;
import com.zj.aiagent.shared.design.workflow.NodeExecutor;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import lombok.Builder;
import lombok.Data;

/**
 * 节点执行上下文
 * <p>
 * 携带节点执行所需的所有信息
 * </p>
 */
@Data
@Builder
public class NodeExecutionContext {
    /**
     * 节点 ID
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 工作流状态
     */
    private WorkflowState state;

    /**
     * 节点执行器
     */
    private NodeExecutor executor;

    /**
     * 节点配置（包含所有拦截器配置）
     */
    private JSONObject nodeConfig;

    /**
     * 当前重试次数
     */
    private int currentRetryCount;

    /**
     * 获取指定类型的配置
     *
     * @param key   配置键
     * @param clazz 配置类型
     * @param <T>   泛型类型
     * @return 配置对象，如果不存在则返回 null
     */
    public <T> T getConfig(String key, Class<T> clazz) {
        if (nodeConfig == null || !nodeConfig.containsKey(key)) {
            return null;
        }
        return nodeConfig.getObject(key, clazz);
    }
}
