package com.zj.aiagent.shared.design.dag;

import java.util.Collections;
import java.util.Set;

/**
 * 节点路由决策 - 条件节点的执行结果
 */
public class NodeRouteDecision {

    private final Set<String> nextNodeIds;
    private final boolean stopExecution;
    private final boolean isLoopBack;

    private NodeRouteDecision(Set<String> nextNodeIds, boolean stopExecution, boolean isLoopBack) {
        this.nextNodeIds = nextNodeIds != null ? nextNodeIds : Collections.emptySet();
        this.stopExecution = stopExecution;
        this.isLoopBack = isLoopBack;
    }

    /**
     * 继续执行指定的节点
     */
    public static NodeRouteDecision continueWith(Set<String> nextNodeIds) {
        return new NodeRouteDecision(nextNodeIds, false, false);
    }

    /**
     * 继续执行单个节点
     */
    public static NodeRouteDecision continueWith(String nextNodeId) {
        return new NodeRouteDecision(Set.of(nextNodeId), false, false);
    }

    /**
     * 停止DAG执行
     */
    public static NodeRouteDecision stop() {
        return new NodeRouteDecision(Collections.emptySet(), true, false);
    }

    /**
     * 继续执行所有候选节点
     */
    public static NodeRouteDecision continueAll() {
        return new NodeRouteDecision(null, false, false);
    }

    /**
     * 循环回到指定节点（用于已执行过的节点）
     */
    public static NodeRouteDecision loopBack(String targetNodeId) {
        return new NodeRouteDecision(Set.of(targetNodeId), false, true);
    }

    public Set<String> getNextNodeIds() {
        return nextNodeIds;
    }

    public boolean isStopExecution() {
        return stopExecution;
    }

    public boolean isLoopBack() {
        return isLoopBack;
    }
}
