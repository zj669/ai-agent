package com.zj.aiagent.domain.workflow.entity;

import com.zj.aiagent.shared.design.workflow.NodeExecutor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAG图模型 - 包含所有节点和依赖关系
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowGraph {

    /**
     * DAG ID
     */
    private String dagId;

    /**
     * 节点映射 (nodeId -> node)
     */
    private Map<String, NodeExecutor> nodes;

    /**
     * 边列表
     */
    private List<EdgeDefinitionEntity> edges;

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
    public NodeExecutor getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 获取节点的依赖列表
     */
    public List<String> getNodeDependencies(String nodeId) {
        return dependencies.getOrDefault(nodeId, List.of());
    }

    /**
     * 获取从指定节点出发的所有边
     * 
     * @param nodeId 源节点ID
     * @return Map<目标节点ID, 边定义>
     */
    public Map<String, EdgeDefinitionEntity> getNextNodes(String nodeId) {
        Map<String, EdgeDefinitionEntity> result = new HashMap<>();
        if (edges != null) {
            for (EdgeDefinitionEntity edge : edges) {
                if (nodeId.equals(edge.getSource())) {
                    result.put(edge.getTarget(), edge);
                }
            }
        }
        return result;
    }
}
