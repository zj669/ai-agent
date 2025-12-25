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
        for (String nodeId : dependencies.keySet()) {
            pendingDependencies.put(nodeId, new AtomicInteger(dependencies.get(nodeId).size()));
            downstreamNodes.put(nodeId, new HashSet<>(dependencies.get(nodeId)));
        }
    }

    public Set<String> getReadyNodes() {
        Set<String> readyNodes = new HashSet<>();
        for (String nodeId : pendingDependencies.keySet()) {
            if (pendingDependencies.get(nodeId).get() == 0 && !completedNodes.contains(nodeId)) {
                readyNodes.add(nodeId);
            }
        }
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
     * 检查节点是否已完成
     */
    public boolean isCompleted(String nodeId) {
        return completedNodes.contains(nodeId);
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
     */
    public boolean isAllCompleted() {
        for (String nodeId : pendingDependencies.keySet()) {
            if (!completedNodes.contains(nodeId)) {
                return false;
            }
        }
        return true;
    }
}
