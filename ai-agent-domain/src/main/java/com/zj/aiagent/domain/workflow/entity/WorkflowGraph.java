package com.zj.aiagent.domain.workflow.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流图实体
 * 表示 DAG 结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowGraph {

    /**
     * 图ID
     */
    private String graphId;

    /**
     * 图版本
     */
    private String version;

    /**
     * 图描述
     */
    private String description;

    /**
     * 节点映射 (nodeId -> Node)
     */
    @Builder.Default
    private Map<String, Node> nodes = new HashMap<>();

    /**
     * 边定义 (sourceNodeId -> Set<targetNodeId>)
     * 用于快速查找下游节点
     */
    @Builder.Default
    private Map<String, Set<String>> edges = new HashMap<>();

    /**
     * 边详情映射 (sourceNodeId -> List<Edge>)
     * 存储边的完整信息，包括条件表达式
     */
    @Builder.Default
    private Map<String, List<Edge>> edgeDetails = new HashMap<>();

    // --- 业务方法 ---

    /**
     * 获取节点的所有出边（包含条件信息）
     */
    public List<Edge> getOutgoingEdges(String nodeId) {
        return edgeDetails.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * 获取节点
     */
    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 获取下游节点
     */
    public Set<Node> getSuccessors(String nodeId) {
        Set<String> successorIds = edges.getOrDefault(nodeId, Collections.emptySet());
        return successorIds.stream()
                .map(nodes::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 获取上游节点
     */
    public Set<Node> getPredecessors(String nodeId) {
        return edges.entrySet().stream()
                .filter(entry -> entry.getValue().contains(nodeId))
                .map(entry -> nodes.get(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 检测是否有环
     * 
     * @return true 表示存在环
     */
    public boolean hasCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String nodeId : nodes.keySet()) {
            if (hasCycleDFS(nodeId, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycleDFS(String nodeId, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true; // 发现环
        }
        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);

        Set<String> successors = edges.getOrDefault(nodeId, Collections.emptySet());
        for (String successor : successors) {
            if (hasCycleDFS(successor, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }
}
