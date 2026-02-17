package com.zj.aiagent.domain.workflow.entity;

import com.zj.aiagent.domain.workflow.valobj.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 执行聚合根
 * 管理工作流执行生命周期、状态流转和检查点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Execution {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 关联的 Assistant 消息 ID
     * 用于在 workflow 执行完成后更新消息内容
     */
    private String assistantMessageId;

    /**
     * 工作流图
     */
    private WorkflowGraph graph;

    /**
     * 执行上下文
     */
    @Builder.Default
    private ExecutionContext context = new ExecutionContext();

    /**
     * 整体执行状态
     */
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    /**
     * 节点状态映射 (nodeId -> status)
     */
    @Builder.Default
    private Map<String, ExecutionStatus> nodeStatuses = new HashMap<>();

    /**
     * 当前暂停的节点ID
     */
    private String pausedNodeId;

    /**
     * 当前暂停的阶段
     */
    private TriggerPhase pausedPhase;

    /**
     * 乐观锁版本
     */
    @Builder.Default
    private Integer version = 0;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // --- 核心业务方法 ---

    /**
     * 启动执行
     */
    public List<Node> start(Map<String, Object> inputs) {
        // 1. 校验图结构
        if (graph.hasCycle()) {
            throw new IllegalStateException("工作流图存在循环依赖");
        }

        // 2. 初始化上下文
        context.setInputs(inputs);

        // 3. 初始化所有节点状态
        graph.getNodes().keySet().forEach(nodeId -> nodeStatuses.put(nodeId, ExecutionStatus.PENDING));

        // 4. 更新状态
        this.status = ExecutionStatus.RUNNING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // 5. 返回就绪节点（入度为0的节点）
        return getReadyNodes();
    }

    /**
     * 推进执行（节点完成后调用）
     * 
     * @return 下一批可执行的节点
     */
    public List<Node> advance(String nodeId, NodeExecutionResult result) {
        // 1. 更新节点状态
        nodeStatuses.put(nodeId, result.getStatus());

        // 2. 存储输出到上下文
        if (result.getOutputs() != null) {
            context.setNodeOutput(nodeId, result.getOutputs());
        }

        // 3. 处理条件分支（剪枝）
        if (result.isRouting()) {
            pruneUnselectedBranches(nodeId, result.getSelectedBranchId());
        }

        // 4. 检查是否暂停
        if (result.isPaused()) {
            this.status = ExecutionStatus.PAUSED_FOR_REVIEW;
            this.pausedNodeId = nodeId;
            this.pausedPhase = result.getTriggerPhase();
            return Collections.emptyList();
        }

        // 5. 更新时间和版本
        this.updatedAt = LocalDateTime.now();
        this.version++;

        // 6. 检查是否完成
        if (isCompleted()) {
            this.status = ExecutionStatus.SUCCEEDED;
            return Collections.emptyList();
        }

        if (hasFailed()) {
            this.status = ExecutionStatus.FAILED;
            return Collections.emptyList();
        }

        // 7. 返回下一批就绪节点
        return getReadyNodes();
    }

    /**
     * 恢复执行（人工审核通过后）
     */
    public List<Node> resume(String nodeId, Map<String, Object> additionalInputs) {
        if (this.status != ExecutionStatus.PAUSED_FOR_REVIEW && this.status != ExecutionStatus.PAUSED) {
            throw new IllegalStateException("当前执行未处于暂停状态");
        }
        if (this.pausedNodeId != null && !this.pausedNodeId.equals(nodeId)) {
            throw new IllegalArgumentException("恢复节点不匹配，期望: " + this.pausedNodeId + ", 实际: " + nodeId);
        }

        // 合并额外输入 (通常由 SchedulerService 处理 Context 具体更新，这里作为备份)
        if (additionalInputs != null) {
            context.getSharedState().putAll(additionalInputs);
        }

        TriggerPhase phase = this.pausedPhase;
        // 兼容旧数据
        if (phase == null) {
            phase = TriggerPhase.AFTER_EXECUTION;
        }

        // 重置状态
        this.status = ExecutionStatus.RUNNING;
        this.pausedNodeId = null;
        this.pausedPhase = null;
        this.updatedAt = LocalDateTime.now();
        this.version++;

        Node pausedNode = graph.getNode(nodeId);
        if (pausedNode == null)
            return Collections.emptyList();

        // If BEFORE_EXECUTION, return node to be executed.
        // If AFTER_EXECUTION, return empty (Scheduler will manually advance).
        return (phase == TriggerPhase.BEFORE_EXECUTION) ? List.of(pausedNode) : Collections.emptyList();
    }
    // update inputs
    // execution.resume() -> returns [node]
    // schedule(node)
    // if AFTER:
    // update outputs
    // execution.resume() -> resets status
    // // We need to simulate node completion
    // execution.advance(nodeId, success(outputs))
    // schedule(nextNodes)

    // So execution.resume() should just reset status.
    // But existing execution.resume() returns List<Node>.
    // I will implement logic to return current node for BEFORE, and empty list for
    // AFTER (caller handles advance).

    // return(phase==TriggerPhase.BEFORE_EXECUTION)?List.of(pausedNode):Collections.emptyList();
    //
    // }

    /**
     * 获取就绪节点（所有依赖已完成）
     */
    public List<Node> getReadyNodes() {
        Map<String, Integer> inDegrees = calculateEffectiveInDegrees();

        return graph.getNodes().values().stream()
                .filter(node -> nodeStatuses.get(node.getNodeId()) == ExecutionStatus.PENDING)
                .filter(node -> inDegrees.getOrDefault(node.getNodeId(), 0) == 0)
                .collect(Collectors.toList());
    }

    /**
     * 计算有效入度（考虑已完成和已跳过的节点）
     */
    private Map<String, Integer> calculateEffectiveInDegrees() {
        Map<String, Integer> inDegrees = new HashMap<>();

        // 初始化
        graph.getNodes().keySet().forEach(nodeId -> inDegrees.put(nodeId, 0));

        // 计算入度
        for (Map.Entry<String, Set<String>> entry : graph.getEdges().entrySet()) {
            String sourceId = entry.getKey();
            ExecutionStatus sourceStatus = nodeStatuses.get(sourceId);

            // 如果源节点已完成或已跳过，不计入入度
            if (sourceStatus == ExecutionStatus.SUCCEEDED ||
                    sourceStatus == ExecutionStatus.SKIPPED ||
                    sourceStatus == ExecutionStatus.FAILED) {
                continue;
            }

            for (String targetId : entry.getValue()) {
                inDegrees.merge(targetId, 1, Integer::sum);
            }
        }

        return inDegrees;
    }

    /**
     * 剪枝未选中的分支
     * selectedBranchId 是条件节点选中的目标节点 ID，直接与后继节点 ID 比较
     */
    private void pruneUnselectedBranches(String conditionNodeId, String selectedBranchId) {
        Node conditionNode = graph.getNode(conditionNodeId);
        if (conditionNode == null || !conditionNode.isConditionNode()) {
            return;
        }

        Set<Node> successors = graph.getSuccessors(conditionNodeId);

        for (Node successor : successors) {
            // 直接比较 successor.nodeId 与 selectedBranchId
            if (!successor.getNodeId().equals(selectedBranchId)) {
                skipNodeRecursively(successor.getNodeId());
            }
        }
    }

    /**
     * 递归跳过节点
     * 汇聚节点（多前驱）仅当所有前驱都是 SKIPPED 时才跳过；
     * 如果有任何前驱是 PENDING，不跳过，等待其他分支完成
     */
    private void skipNodeRecursively(String nodeId) {
        ExecutionStatus currentStatus = nodeStatuses.get(nodeId);

        // 仅跳过待执行的节点
        if (currentStatus != ExecutionStatus.PENDING) {
            return;
        }

        nodeStatuses.put(nodeId, ExecutionStatus.SKIPPED);

        // 递归跳过下游
        Set<Node> successors = graph.getSuccessors(nodeId);
        for (Node successor : successors) {
            Set<Node> predecessors = graph.getPredecessors(successor.getNodeId());

            if (predecessors.size() <= 1) {
                // 单前驱：直接递归跳过
                skipNodeRecursively(successor.getNodeId());
            } else {
                // 汇聚节点：仅当所有前驱都是 SKIPPED 时才跳过
                boolean allPredecessorsSkipped = predecessors.stream()
                        .allMatch(pred -> nodeStatuses.get(pred.getNodeId()) == ExecutionStatus.SKIPPED);
                if (allPredecessorsSkipped) {
                    skipNodeRecursively(successor.getNodeId());
                }
                // 否则不跳过，等待其他分支的前驱完成
            }
        }
    }

    /**
     * 检查是否完成
     */
    private boolean isCompleted() {
        return nodeStatuses.values().stream()
                .allMatch(status -> status == ExecutionStatus.SUCCEEDED ||
                        status == ExecutionStatus.SKIPPED ||
                        status == ExecutionStatus.FAILED);
    }

    /**
     * 检查是否有失败节点
     */
    private boolean hasFailed() {
        return nodeStatuses.values().stream()
                .anyMatch(status -> status == ExecutionStatus.FAILED);
    }

    /**
     * 创建检查点
     */
    public Checkpoint createCheckpoint(String nodeId) {
        if (this.status == ExecutionStatus.PAUSED || this.status == ExecutionStatus.PAUSED_FOR_REVIEW) {
            return Checkpoint.createPausePoint(executionId, nodeId, context);
        }
        return Checkpoint.create(executionId, nodeId, context);
    }
}
