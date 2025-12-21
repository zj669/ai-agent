package com.zj.aiagent.shared.design.dag;

import java.util.Collections;
import java.util.Set;

/**
 * 节点路由决策 - 条件节点的执行结果
 */
public class NodeRouteDecision {

    private final Set<String> nextNodeIds;
    private final boolean stopExecution;

    private NodeRouteDecision(Set<String> nextNodeIds, boolean stopExecution) {
        this.nextNodeIds = nextNodeIds != null ? nextNodeIds : Collections.emptySet();
        this.stopExecution = stopExecution;
    }

    /**
     * 继续执行指定的节点
     */
    public static NodeRouteDecision continueWith(Set<String> nextNodeIds) {
        return new NodeRouteDecision(nextNodeIds, false);
    }

    /**
     * 继续执行单个节点
     */
    public static NodeRouteDecision continueWith(String nextNodeId) {
        return new NodeRouteDecision(Set.of(nextNodeId), false);
    }

    /**
     * 停止DAG执行
     */
    public static NodeRouteDecision stop() {
        return new NodeRouteDecision(Collections.emptySet(), true);
    }

    /**
     * 继续执行所有候选节点
     */
    public static NodeRouteDecision continueAll() {
        return new NodeRouteDecision(null, false);
    }

    public Set<String> getNextNodeIds() {
        return nextNodeIds;
    }

    public boolean isStopExecution() {
        return stopExecution;
    }
}
