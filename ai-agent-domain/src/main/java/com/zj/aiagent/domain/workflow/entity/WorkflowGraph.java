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
     */
    @Builder.Default
    private Map<String, Set<String>> edges = new HashMap<>();

    // --- 业务方法 ---

    /**
     * 获取所有起始节点
     */
    public List<Node> getStartNodes() {
        return nodes.values().stream()
                .filter(Node::isStartNode)
                .collect(Collectors.toList());
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
     * 计算节点入度
     */
    public Map<String, Integer> calculateInDegrees() {
        Map<String, Integer> inDegrees = new HashMap<>();

        // 初始化所有节点入度为 0
        nodes.keySet().forEach(nodeId -> inDegrees.put(nodeId, 0));

        // 统计入度
        edges.values().forEach(targets -> targets.forEach(target -> inDegrees.merge(target, 1, Integer::sum)));

        return inDegrees;
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

    /**
     * 获取拓扑排序
     */
    public List<String> topologicalSort() {
        Map<String, Integer> inDegrees = calculateInDegrees();
        Queue<String> queue = new LinkedList<>();
        List<String> result = new ArrayList<>();

        // 入度为0的节点入队
        inDegrees.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .map(Map.Entry::getKey)
                .forEach(queue::add);

        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            result.add(nodeId);

            Set<String> successors = edges.getOrDefault(nodeId, Collections.emptySet());
            for (String successor : successors) {
                int newDegree = inDegrees.merge(successor, -1, Integer::sum);
                if (newDegree == 0) {
                    queue.add(successor);
                }
            }
        }

        return result;
    }
}
