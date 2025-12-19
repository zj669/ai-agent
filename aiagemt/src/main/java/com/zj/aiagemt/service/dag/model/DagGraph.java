package com.zj.aiagemt.service.dag.model;

import com.zj.aiagemt.common.design.dag.DagNode;
import com.zj.aiagemt.service.dag.context.DagExecutionContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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
     */
    private Map<String, DagNode<DagExecutionContext, String>> nodes;

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
    public DagNode<DagExecutionContext, String> getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 获取节点的依赖列表
     */
    public List<String> getNodeDependencies(String nodeId) {
        return dependencies.getOrDefault(nodeId, List.of());
    }
}
