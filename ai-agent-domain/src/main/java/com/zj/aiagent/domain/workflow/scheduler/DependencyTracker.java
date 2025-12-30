package com.zj.aiagent.domain.workflow.scheduler;

import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
public class DependencyTracker {
    /**
     * 待处理的依赖项
     */
    private final ConcurrentHashMap<String, AtomicInteger> pendingDependencies;

    /**
     * 下游节点
     */
    private final Map<String, Set<String>> downstreamNodes;
    /**
     * 已完成的节点
     */
    private final Set<String> completedNodes;

    public DependencyTracker(WorkflowGraph graph) {
        pendingDependencies = new ConcurrentHashMap<>();
        downstreamNodes = new HashMap<>();
        completedNodes = new HashSet<>();
        init(graph);
    }

    public void init(WorkflowGraph graph) {
        Map<String, List<String>> dependencies = graph.getDependencies();

        log.info("开始初始化 DependencyTracker, 节点总数={}, dependencies.size={}",
                graph.getNodes().size(), dependencies.size());

        // 初始化所有节点的依赖计数
        for (String nodeId : graph.getNodes().keySet()) {
            List<String> nodeDeps = dependencies.getOrDefault(nodeId, new ArrayList<>());
            pendingDependencies.put(nodeId, new AtomicInteger(nodeDeps.size()));
            log.debug("节点 {} 的依赖数: {}, 依赖列表: {}", nodeId, nodeDeps.size(), nodeDeps);
        }

        // 反向构建下游节点映射：对于每个节点的每个依赖，在依赖节点的下游列表中添加当前节点
        for (Map.Entry<String, List<String>> entry : dependencies.entrySet()) {
            String nodeId = entry.getKey();
            List<String> nodeDeps = entry.getValue();

            for (String dependency : nodeDeps) {
                downstreamNodes.computeIfAbsent(dependency, k -> new HashSet<>()).add(nodeId);
            }
        }

        // 查找起始节点
        List<String> startNodes = pendingDependencies.entrySet().stream()
                .filter(e -> e.getValue().get() == 0)
                .map(Map.Entry::getKey)
                .toList();

        log.info("DependencyTracker 初始化完成: 节点总数={}, 起始节点数={}, 起始节点={}",
                pendingDependencies.size(), startNodes.size(), startNodes);
    }

    public Set<String> getReadyNodes() {
        Set<String> readyNodes = new HashSet<>();
        for (String nodeId : pendingDependencies.keySet()) {
            if (pendingDependencies.get(nodeId).get() == 0 && !completedNodes.contains(nodeId)) {
                readyNodes.add(nodeId);
            }
        }
        log.debug("getReadyNodes() 返回: {} (总节点数={}, 已完成数={})",
                readyNodes, pendingDependencies.size(), completedNodes.size());
        return readyNodes;
    }

    public Set<String> markCompleted(String nodeId) {
        if (completedNodes.contains(nodeId)) {
            log.warn("节点已标记为完成: {}", nodeId);
            return Collections.emptySet();
        }
        completedNodes.add(nodeId); // ← 缺少这行！

        Set<String> newReadyNodes = new HashSet<>();
        Set<String> downstream = downstreamNodes.get(nodeId);
        if (downstream != null) {
            for (String downstreamNodeId : downstream) {
                AtomicInteger counter = pendingDependencies.get(downstreamNodeId);
                int newCount = counter.decrementAndGet();

                log.debug("下游节点 {} 依赖计数: {} -> {}", downstreamNodeId, newCount + 1, newCount);

                // 依赖归零且未完成，加入就绪队列
                if (newCount == 0 && !completedNodes.contains(downstreamNodeId)) {
                    newReadyNodes.add(downstreamNodeId);
                    log.info("节点 {} 依赖归零，加入就绪队列", downstreamNodeId);
                }
            }
        }
        return newReadyNodes;
    }

    public int getCompletedCount() {
        return completedNodes.size();
    }

    /**
     * 仅标记节点为已完成，不传播到下游节点
     * <p>
     * 用于恢复执行时标记已经完成的上游节点，
     * 避免重复触发下游节点的依赖计数变化
     */
    public void markCompletedWithoutPropagate(String nodeId) {
        if (completedNodes.contains(nodeId)) {
            log.debug("节点已标记为完成（无传播）: {}", nodeId);
            return;
        }
        completedNodes.add(nodeId);
        log.debug("标记节点完成（无传播）: {}", nodeId);
    }

    /**
     * 检查节点是否已完成
     */
    public boolean isCompleted(String nodeId) {
        return completedNodes.contains(nodeId);
    }

    /**
     * 检查节点是否存在于跟踪器中
     */
    public boolean hasNode(String nodeId) {
        return pendingDependencies.containsKey(nodeId);
    }

    /**
     * 重置节点状态以支持循环执行
     */
    public void resetForLoop(String nodeId) {
        if (completedNodes.remove(nodeId)) {
            log.info("重置节点状态以支持循环执行: {}", nodeId);
            // 注意：不重置依赖计数，因为循环节点的依赖已经满足
        }
    }

    /**
     * 检查是否所有节点都已完成
     * <p>
     * ⭐ 修改逻辑以支持条件分支：
     * - 原逻辑：检查图中定义的所有节点是否都完成（不适合条件分支）
     * - 新逻辑：检查是否还有可执行的节点（依赖已满足但未完成的节点）
     * <p>
     * 工作流完成条件：没有就绪节点 && 没有运行中的节点
     */
    public boolean isAllCompleted() {
        // 检查是否还有依赖已满足但未完成的节点
        for (Map.Entry<String, AtomicInteger> entry : pendingDependencies.entrySet()) {
            String nodeId = entry.getKey();
            int dependencyCount = entry.getValue().get();

            // 如果节点依赖已满足（dependencyCount == 0）但还未完成，说明有节点可执行
            if (dependencyCount == 0 && !completedNodes.contains(nodeId)) {
                log.debug("节点 {} 依赖已满足但未完成，工作流未完成", nodeId);
                return false;
            }
        }

        // 所有依赖已满足的节点都已完成，工作流完成
        log.debug("所有可执行节点都已完成，工作流完成。已完成节点: {}/{}",
                completedNodes.size(), pendingDependencies.size());
        return true;
    }
}
