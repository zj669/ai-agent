package com.zj.aiagent.domain.agent.dag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import com.zj.aiagent.domain.agent.dag.node.AbstractConfigurableNode;

/**
 * DAG图模型 - 包含所有节点和依赖关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DagGraph {

    /**
     * DAG ID
     */
    private String dagId;

    /**
     * 版本
     */
    private String version;

    /**
     * 描述
     */
    private String description;

    /**
     * 节点映射 (nodeId -> node)
     * 统一使用 AbstractConfigurableNode 类型
     */
    private Map<String, AbstractConfigurableNode> nodes;

    /**
     * 边列表
     */
    private List<GraphJsonSchema.EdgeDefinition> edges;

    /**
     * 依赖关系 (nodeId -> 依赖的节点ID列表)
     */
    private Map<String, List<String>> dependencies;

    /**
     * 起始节点ID
     */
    private String startNodeId;

    /**
     * 获取节点
     */
    public AbstractConfigurableNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 获取节点的依赖列表
     */
    public List<String> getNodeDependencies(String nodeId) {
        return dependencies.getOrDefault(nodeId, List.of());
    }
}
