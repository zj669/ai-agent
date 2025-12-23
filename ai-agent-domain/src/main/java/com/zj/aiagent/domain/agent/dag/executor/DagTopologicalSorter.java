package com.zj.aiagent.domain.agent.dag.executor;

import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.entity.EdgeType;
import com.zj.aiagent.domain.agent.dag.exception.NodeConfigException;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * DAG拓扑排序工具
 * 用于检测循环依赖和确定节点执行顺序
 */
@Slf4j
public class DagTopologicalSorter {

    /**
     * 执行拓扑排序
     * 
     * @param dagGraph DAG图
     * @return 排序后的节点ID列表
     * @throws NodeConfigException 如果存在循环依赖
     */
    public static List<String> sort(DagGraph dagGraph) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacencyList = new HashMap<>();

        // 初始化入度和邻接表
        for (String nodeId : dagGraph.getNodes().keySet()) {
            inDegree.put(nodeId, 0);
            adjacencyList.put(nodeId, new ArrayList<>());
        }

        // 构建邻接表和计算入度（忽略 LOOP_BACK 边）
        for (var edge : dagGraph.getEdges()) {
            // 跳过循环边，循环边不参与拓扑排序
            if (edge.getEdgeType() == EdgeType.LOOP_BACK) {
                log.debug("跳过循环边: {} -> {}", edge.getSource(), edge.getTarget());
                continue;
            }

            String source = edge.getSource();
            String target = edge.getTarget();

            adjacencyList.get(source).add(target);
            inDegree.put(target, inDegree.get(target) + 1);
        }

        // 找出所有入度为0的节点
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<String> sortedNodes = new ArrayList<>();

        // Kahn算法进行拓扑排序
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sortedNodes.add(current);

            // 减少相邻节点的入度
            for (String neighbor : adjacencyList.get(current)) {
                int newInDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newInDegree);

                if (newInDegree == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // 检查是否存在循环依赖
        if (sortedNodes.size() != dagGraph.getNodes().size()) {
            log.error("DAG中存在循环依赖，已排序节点数: {}, 总节点数: {}",
                    sortedNodes.size(), dagGraph.getNodes().size());
            throw new NodeConfigException("Cyclic dependency detected in DAG");
        }

        log.info("拓扑排序完成，节点顺序: {}", sortedNodes);
        return sortedNodes;
    }

    /**
     * 检测循环依赖（使用DFS）
     */
    public static boolean hasCycle(DagGraph dagGraph) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String nodeId : dagGraph.getNodes().keySet()) {
            if (hasCycleDFS(nodeId, dagGraph, visited, recursionStack)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasCycleDFS(String nodeId, DagGraph dagGraph,
            Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true; // 发现循环
        }

        if (visited.contains(nodeId)) {
            return false; // 已访问过
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);

        // 访问所有依赖节点
        List<String> dependencies = dagGraph.getNodeDependencies(nodeId);
        for (String dep : dependencies) {
            if (hasCycleDFS(dep, dagGraph, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }

    /**
     * 获取可以并行执行的节点层级
     * 返回结果是一个列表，每个元素是一层可以并行执行的节点
     */
    public static List<List<String>> getExecutionLevels(DagGraph dagGraph) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacencyList = new HashMap<>();

        // 初始化
        for (String nodeId : dagGraph.getNodes().keySet()) {
            inDegree.put(nodeId, 0);
            adjacencyList.put(nodeId, new ArrayList<>());
        }

        // 构建邻接表和计算入度（忽略 LOOP_BACK 边）
        for (var edge : dagGraph.getEdges()) {
            // 跳过循环边
            if (edge.getEdgeType() == EdgeType.LOOP_BACK) {
                continue;
            }

            adjacencyList.get(edge.getSource()).add(edge.getTarget());
            inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
        }

        List<List<String>> levels = new ArrayList<>();
        Queue<String> currentLevel = new LinkedList<>();

        // 第一层：所有入度为0的节点
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                currentLevel.offer(entry.getKey());
            }
        }

        while (!currentLevel.isEmpty()) {
            List<String> level = new ArrayList<>(currentLevel);
            levels.add(level);

            Queue<String> nextLevel = new LinkedList<>();

            for (String nodeId : level) {
                for (String neighbor : adjacencyList.get(nodeId)) {
                    int newInDegree = inDegree.get(neighbor) - 1;
                    inDegree.put(neighbor, newInDegree);

                    if (newInDegree == 0) {
                        nextLevel.offer(neighbor);
                    }
                }
            }

            currentLevel = nextLevel;
        }

        log.info("DAG执行层级: {} 层", levels.size());
        for (int i = 0; i < levels.size(); i++) {
            log.info("第 {} 层: {}", i + 1, levels.get(i));
        }

        return levels;
    }
}
