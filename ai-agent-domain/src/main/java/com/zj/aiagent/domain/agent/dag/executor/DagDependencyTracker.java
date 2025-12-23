package com.zj.aiagent.domain.agent.dag.executor;

import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.entity.EdgeType;
import com.zj.aiagent.domain.agent.dag.entity.GraphJsonSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAG依赖跟踪器
 * 负责跟踪每个节点的依赖状态，支持细粒度并行调度
 */
@Slf4j
public class DagDependencyTracker {

    /**
     * 节点 -> 待完成依赖数（原子操作保证线程安全）
     */
    private final ConcurrentHashMap<String, AtomicInteger> pendingDependencies;

    /**
     * 节点 -> 下游节点列表（正向依赖图）
     */
    private final Map<String, Set<String>> downstreamNodes;

    /**
     * 已完成的节点集合
     */
    private final Set<String> completedNodes;

    /**
     * 启用的节点集合（用于路由过滤）
     */
    private final Set<String> enabledNodes;

    /**
     * 循环边映射 (source -> target集合)
     * 存储所有 LOOP_BACK 类型的边，用于运行时触发循环
     */
    private final Map<String, Set<String>> loopBackEdges;

    /**
     * 构造函数
     * 
     * @param dagGraph DAG图
     */
    public DagDependencyTracker(DagGraph dagGraph) {
        this.pendingDependencies = new ConcurrentHashMap<>();
        this.downstreamNodes = new HashMap<>();
        this.completedNodes = Collections.synchronizedSet(new HashSet<>());
        this.enabledNodes = Collections.synchronizedSet(new HashSet<>());
        this.loopBackEdges = new HashMap<>();

        initializeDependencies(dagGraph);
    }

    /**
     * 初始化依赖关系
     */
    private void initializeDependencies(DagGraph dagGraph) {
        // 初始化所有节点的依赖计数和下游节点列表
        for (String nodeId : dagGraph.getNodes().keySet()) {
            pendingDependencies.put(nodeId, new AtomicInteger(0));
            downstreamNodes.put(nodeId, new HashSet<>());
            enabledNodes.add(nodeId);
        }

        // 根据边构建依赖关系（区分依赖边和循环边）
        for (GraphJsonSchema.EdgeDefinition edge : dagGraph.getEdges()) {
            String source = edge.getSource();
            String target = edge.getTarget();

            // 处理循环边
            if (edge.getEdgeType() == EdgeType.LOOP_BACK) {
                loopBackEdges.computeIfAbsent(source, k -> new HashSet<>()).add(target);
                log.debug("记录循环边: {} -> {}", source, target);
                continue; // 循环边不参与依赖计数
            }

            // 处理标准依赖边
            // 目标节点的依赖计数+1
            pendingDependencies.get(target).incrementAndGet();

            // 将目标节点添加到源节点的下游列表
            downstreamNodes.get(source).add(target);
        }

        log.info("依赖跟踪器初始化完成，节点数: {}, 循环边数: {}",
                dagGraph.getNodes().size(), loopBackEdges.size());
        logDependencyStatus();
    }

    /**
     * 获取初始可执行节点（依赖计数为0且未完成的节点）
     * 
     * @return 可执行节点ID集合
     */
    public Set<String> getReadyNodes() {
        Set<String> readyNodes = new HashSet<>();

        for (Map.Entry<String, AtomicInteger> entry : pendingDependencies.entrySet()) {
            String nodeId = entry.getKey();
            int pending = entry.getValue().get();

            // 依赖计数为0、未完成、且未被路由禁用
            if (pending == 0 && !completedNodes.contains(nodeId) && enabledNodes.contains(nodeId)) {
                readyNodes.add(nodeId);
            }
        }

        log.debug("获取就绪节点: {}", readyNodes);
        return readyNodes;
    }

    /**
     * 标记节点完成，返回因此而依赖归零的下游节点
     * 
     * @param nodeId 完成的节点ID
     * @return 新就绪的节点ID集合
     */
    public Set<String> markCompleted(String nodeId) {
        if (completedNodes.contains(nodeId)) {
            log.warn("节点已标记为完成: {}", nodeId);
            return Collections.emptySet();
        }

        completedNodes.add(nodeId);
        log.debug("标记节点完成: {}", nodeId);

        Set<String> newReadyNodes = new HashSet<>();

        // 减少所有下游节点的依赖计数
        Set<String> downstream = downstreamNodes.get(nodeId);
        if (downstream != null) {
            for (String downstreamNodeId : downstream) {
                // 只处理启用的节点
                if (!enabledNodes.contains(downstreamNodeId)) {
                    continue;
                }

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

    /**
     * 禁用节点（用于路由过滤）
     * 
     * @param nodeId 要禁用的节点ID
     */
    public void disableNode(String nodeId) {
        if (enabledNodes.remove(nodeId)) {
            log.info("禁用节点: {}", nodeId);

            // 递归禁用所有下游节点
            Set<String> downstream = downstreamNodes.get(nodeId);
            if (downstream != null) {
                for (String downstreamNodeId : downstream) {
                    disableNode(downstreamNodeId);
                }
            }
        }
    }

    /**
     * 禁用多个节点及其下游
     * 
     * @param nodeIds 要禁用的节点ID集合
     */
    public void disableNodes(Set<String> nodeIds) {
        for (String nodeId : nodeIds) {
            disableNode(nodeId);
        }
    }

    /**
     * 检查是否所有启用的节点都已完成
     * 
     * @return true 如果所有启用节点都已完成
     */
    public boolean isAllCompleted() {
        for (String nodeId : enabledNodes) {
            if (!completedNodes.contains(nodeId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取已完成节点数
     */
    public int getCompletedCount() {
        return completedNodes.size();
    }

    /**
     * 获取启用节点数
     */
    public int getEnabledCount() {
        return enabledNodes.size();
    }

    /**
     * 记录依赖状态（调试用）
     */
    private void logDependencyStatus() {
        for (Map.Entry<String, AtomicInteger> entry : pendingDependencies.entrySet()) {
            String nodeId = entry.getKey();
            int pending = entry.getValue().get();
            Set<String> downstream = downstreamNodes.get(nodeId);

            log.debug("节点 {}: 待完成依赖={}, 下游节点={}",
                    nodeId, pending, downstream != null ? downstream : "[]");
        }
    }

    /**
     * 获取节点的待完成依赖数（仅用于测试/调试）
     */
    public int getPendingDependencies(String nodeId) {
        AtomicInteger counter = pendingDependencies.get(nodeId);
        return counter != null ? counter.get() : -1;
    }

    // ==================== 循环边支持方法 ====================

    /**
     * 重置节点状态，使其可以重新执行
     * 注意：此方法只重置 completedNodes 标记，不重置依赖计数
     * 
     * @param nodeId 要重置的节点ID
     */
    public void resetNode(String nodeId) {
        if (completedNodes.remove(nodeId)) {
            log.info("重置节点状态以支持循环执行: {}", nodeId);
        }
    }

    /**
     * 获取节点的循环边目标节点
     * 
     * @param nodeId 源节点ID
     * @return 循环边指向的目标节点集合，如果没有则返回空集合
     */
    public Set<String> getLoopBackTargets(String nodeId) {
        return loopBackEdges.getOrDefault(nodeId, Collections.emptySet());
    }

    /**
     * 检查节点是否有循环边
     * 
     * @param nodeId 节点ID
     * @return true 如果节点有循环边
     */
    public boolean hasLoopBackEdges(String nodeId) {
        return loopBackEdges.containsKey(nodeId) && !loopBackEdges.get(nodeId).isEmpty();
    }
}
