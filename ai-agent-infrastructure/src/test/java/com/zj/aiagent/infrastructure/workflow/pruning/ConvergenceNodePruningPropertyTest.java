package com.zj.aiagent.infrastructure.workflow.pruning;

import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Execution;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.ExecutionStatus;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;

/**
 * Property-Based Test: 汇聚节点剪枝正确性
 *
 * // Feature: condition-branch-refactor, Property 8: Convergence node pruning correctness
 * **Validates: Requirements 4.2, 4.3**
 *
 * 验证：
 * 对于任意包含汇聚节点（多前驱）的工作流图，Pruning_Engine 仅当汇聚节点的所有前驱
 * 都处于 SKIPPED 状态时才跳过该汇聚节点。如果任何前驱处于 PENDING 或 SUCCEEDED
 * （来自非跳过分支），汇聚节点应保持 PENDING。
 *
 * 测试策略：
 * - 构建菱形 DAG（条件节点 → 多个分支 → 汇聚节点 → END）
 * - 随机选择一个分支作为选中分支
 * - 验证汇聚节点在选中分支经过时保持 PENDING
 * - 验证汇聚节点在所有前驱都被跳过时变为 SKIPPED
 * - 变化前驱数量（2-4 个）验证通用性
 */
class ConvergenceNodePruningPropertyTest {

    // ========== 辅助方法 ==========

    /**
     * 构建菱形 DAG：START → CONDITION → N 个分支节点 → CONVERGENCE → END
     *
     * 结构（以 3 个分支为例）：
     *   start → condition → branch_0 → convergence → end
     *                     → branch_1 →
     *                     → branch_2 →
     *
     * 所有分支节点都汇聚到同一个 convergence 节点。
     *
     * @param branchCount 分支节点数量（即汇聚节点的前驱数量）
     * @return 构建好的 WorkflowGraph
     */
    private WorkflowGraph buildDiamondGraph(int branchCount) {
        Map<String, Node> nodes = new HashMap<>();
        Map<String, Set<String>> edges = new HashMap<>();

        // START 节点
        nodes.put("start", Node.builder()
                .nodeId("start").name("Start").type(NodeType.START)
                .config(NodeConfig.builder().build()).build());

        // CONDITION 节点
        nodes.put("condition", Node.builder()
                .nodeId("condition").name("Condition").type(NodeType.CONDITION)
                .config(NodeConfig.builder().build()).build());

        // CONVERGENCE 节点（汇聚节点，多前驱）
        nodes.put("convergence", Node.builder()
                .nodeId("convergence").name("Convergence").type(NodeType.LLM)
                .config(NodeConfig.builder().build()).build());

        // END 节点
        nodes.put("end", Node.builder()
                .nodeId("end").name("End").type(NodeType.END)
                .config(NodeConfig.builder().build()).build());

        // start → condition
        edges.put("start", new HashSet<>(Set.of("condition")));

        // condition → branch_0, branch_1, ..., branch_{N-1}
        Set<String> conditionSuccessors = new HashSet<>();
        for (int i = 0; i < branchCount; i++) {
            String branchId = "branch_" + i;
            nodes.put(branchId, Node.builder()
                    .nodeId(branchId).name("Branch " + i).type(NodeType.LLM)
                    .config(NodeConfig.builder().build()).build());
            conditionSuccessors.add(branchId);

            // branch_i → convergence
            edges.put(branchId, new HashSet<>(Set.of("convergence")));
        }
        edges.put("condition", conditionSuccessors);

        // convergence → end
        edges.put("convergence", new HashSet<>(Set.of("end")));

        return WorkflowGraph.builder()
                .graphId("diamond-graph")
                .version("1.0")
                .nodes(nodes)
                .edges(edges)
                .build();
    }

    /**
     * 构建全跳过 DAG：START → CONDITION → N 个分支节点 → CONVERGENCE → END
     * 其中还有一个额外的 "selected_target" 节点不经过汇聚节点。
     *
     * 结构（以 3 个分支为例）：
     *   start → condition → branch_0 → convergence → end
     *                     → branch_1 →
     *                     → branch_2 →
     *                     → selected_target → end
     *
     * selected_target 不经过 convergence，所以当它被选中时，
     * 所有 branch_x 都被跳过，convergence 的所有前驱都是 SKIPPED。
     *
     * @param branchCount 汇聚到 convergence 的分支数量
     * @return 构建好的 WorkflowGraph
     */
    private WorkflowGraph buildAllSkippedConvergenceGraph(int branchCount) {
        Map<String, Node> nodes = new HashMap<>();
        Map<String, Set<String>> edges = new HashMap<>();

        // START 节点
        nodes.put("start", Node.builder()
                .nodeId("start").name("Start").type(NodeType.START)
                .config(NodeConfig.builder().build()).build());

        // CONDITION 节点
        nodes.put("condition", Node.builder()
                .nodeId("condition").name("Condition").type(NodeType.CONDITION)
                .config(NodeConfig.builder().build()).build());

        // CONVERGENCE 节点
        nodes.put("convergence", Node.builder()
                .nodeId("convergence").name("Convergence").type(NodeType.LLM)
                .config(NodeConfig.builder().build()).build());

        // SELECTED_TARGET 节点（不经过 convergence 的分支）
        nodes.put("selected_target", Node.builder()
                .nodeId("selected_target").name("Selected Target").type(NodeType.LLM)
                .config(NodeConfig.builder().build()).build());

        // END 节点
        nodes.put("end", Node.builder()
                .nodeId("end").name("End").type(NodeType.END)
                .config(NodeConfig.builder().build()).build());

        // start → condition
        edges.put("start", new HashSet<>(Set.of("condition")));

        // condition → branch_0, ..., branch_{N-1}, selected_target
        Set<String> conditionSuccessors = new HashSet<>();
        conditionSuccessors.add("selected_target");
        for (int i = 0; i < branchCount; i++) {
            String branchId = "branch_" + i;
            nodes.put(branchId, Node.builder()
                    .nodeId(branchId).name("Branch " + i).type(NodeType.LLM)
                    .config(NodeConfig.builder().build()).build());
            conditionSuccessors.add(branchId);

            // branch_i → convergence
            edges.put(branchId, new HashSet<>(Set.of("convergence")));
        }
        edges.put("condition", conditionSuccessors);

        // selected_target → end
        edges.put("selected_target", new HashSet<>(Set.of("end")));

        // convergence → end
        edges.put("convergence", new HashSet<>(Set.of("end")));

        return WorkflowGraph.builder()
                .graphId("all-skipped-graph")
                .version("1.0")
                .nodes(nodes)
                .edges(edges)
                .build();
    }

    /**
     * 创建 Execution 并启动，推进 start 节点完成。
     */
    private Execution createAndStartExecution(WorkflowGraph graph) {
        Execution execution = Execution.builder()
                .executionId("test-exec-" + UUID.randomUUID())
                .graph(graph)
                .context(ExecutionContext.builder().build())
                .build();

        execution.start(new HashMap<>());
        execution.advance("start", NodeExecutionResult.success(Map.of()));

        return execution;
    }

    // ========== Property Tests ==========

    // Feature: condition-branch-refactor, Property 8: Convergence node pruning correctness
    // **Validates: Requirements 4.2, 4.3**
    @Property(tries = 100)
    void convergence_stays_pending_when_selected_branch_goes_through_it(
            @ForAll @IntRange(min = 2, max = 4) int branchCount,
            @ForAll Random random) {

        // 随机选择一个经过汇聚节点的分支
        int selectedIndex = random.nextInt(branchCount);
        String selectedBranchId = "branch_" + selectedIndex;

        WorkflowGraph graph = buildDiamondGraph(branchCount);
        Execution execution = createAndStartExecution(graph);

        // 条件节点选中某个分支，触发剪枝
        execution.advance("condition", NodeExecutionResult.routing(selectedBranchId, Map.of()));

        Map<String, ExecutionStatus> nodeStatuses = execution.getNodeStatuses();

        // 汇聚节点应保持 PENDING，因为选中的分支（PENDING）是它的前驱之一
        assert nodeStatuses.get("convergence") == ExecutionStatus.PENDING :
                String.format("Convergence node should remain PENDING when selected branch '%s' " +
                                "goes through it, but was %s. branchCount=%d, statuses=%s",
                        selectedBranchId, nodeStatuses.get("convergence"),
                        branchCount, getBranchStatuses(nodeStatuses, branchCount));
    }

    // Feature: condition-branch-refactor, Property 8: Convergence node pruning correctness
    // **Validates: Requirements 4.2, 4.3**
    @Property(tries = 100)
    void convergence_becomes_skipped_when_all_predecessors_are_skipped(
            @ForAll @IntRange(min = 2, max = 4) int branchCount) {

        // 选中 selected_target（不经过 convergence），所有 branch_x 都被跳过
        WorkflowGraph graph = buildAllSkippedConvergenceGraph(branchCount);
        Execution execution = createAndStartExecution(graph);

        // 选中不经过汇聚节点的分支
        execution.advance("condition", NodeExecutionResult.routing("selected_target", Map.of()));

        Map<String, ExecutionStatus> nodeStatuses = execution.getNodeStatuses();

        // 所有 branch_x 应为 SKIPPED
        for (int i = 0; i < branchCount; i++) {
            String branchId = "branch_" + i;
            assert nodeStatuses.get(branchId) == ExecutionStatus.SKIPPED :
                    String.format("Branch '%s' should be SKIPPED, but was %s. branchCount=%d",
                            branchId, nodeStatuses.get(branchId), branchCount);
        }

        // 汇聚节点应为 SKIPPED（所有前驱都是 SKIPPED）
        assert nodeStatuses.get("convergence") == ExecutionStatus.SKIPPED :
                String.format("Convergence node should be SKIPPED when all %d predecessors are SKIPPED, " +
                                "but was %s. statuses=%s",
                        branchCount, nodeStatuses.get("convergence"),
                        getBranchStatuses(nodeStatuses, branchCount));
    }

    // Feature: condition-branch-refactor, Property 8: Convergence node pruning correctness
    // **Validates: Requirements 4.2, 4.3**
    @Property(tries = 100)
    void convergence_pending_predecessor_count_matches_exactly_one(
            @ForAll @IntRange(min = 2, max = 4) int branchCount,
            @ForAll Random random) {

        // 在菱形 DAG 中，选中一个分支后，恰好有 1 个前驱是 PENDING，其余是 SKIPPED
        int selectedIndex = random.nextInt(branchCount);
        String selectedBranchId = "branch_" + selectedIndex;

        WorkflowGraph graph = buildDiamondGraph(branchCount);
        Execution execution = createAndStartExecution(graph);

        execution.advance("condition", NodeExecutionResult.routing(selectedBranchId, Map.of()));

        Map<String, ExecutionStatus> nodeStatuses = execution.getNodeStatuses();

        // 统计汇聚节点前驱中 PENDING 和 SKIPPED 的数量
        long pendingPredecessors = 0;
        long skippedPredecessors = 0;
        for (int i = 0; i < branchCount; i++) {
            ExecutionStatus status = nodeStatuses.get("branch_" + i);
            if (status == ExecutionStatus.PENDING) pendingPredecessors++;
            if (status == ExecutionStatus.SKIPPED) skippedPredecessors++;
        }

        assert pendingPredecessors == 1 :
                String.format("Expected exactly 1 PENDING predecessor, got %d. " +
                                "branchCount=%d, selectedBranch=%s",
                        pendingPredecessors, branchCount, selectedBranchId);

        assert skippedPredecessors == branchCount - 1 :
                String.format("Expected %d SKIPPED predecessors, got %d. " +
                                "branchCount=%d, selectedBranch=%s",
                        branchCount - 1, skippedPredecessors, branchCount, selectedBranchId);
    }

    // Feature: condition-branch-refactor, Property 8: Convergence node pruning correctness
    // **Validates: Requirements 4.2, 4.3**
    @Property(tries = 100)
    void selected_target_stays_pending_in_all_skipped_scenario(
            @ForAll @IntRange(min = 2, max = 4) int branchCount) {

        WorkflowGraph graph = buildAllSkippedConvergenceGraph(branchCount);
        Execution execution = createAndStartExecution(graph);

        execution.advance("condition", NodeExecutionResult.routing("selected_target", Map.of()));

        Map<String, ExecutionStatus> nodeStatuses = execution.getNodeStatuses();

        // 选中的 selected_target 应保持 PENDING
        assert nodeStatuses.get("selected_target") == ExecutionStatus.PENDING :
                String.format("Selected target should remain PENDING, but was %s",
                        nodeStatuses.get("selected_target"));
    }

    // Feature: condition-branch-refactor, Property 8: Convergence node pruning correctness
    // **Validates: Requirements 4.2, 4.3**
    @Property(tries = 100)
    void end_node_stays_pending_when_convergence_is_skipped_but_selected_target_is_pending(
            @ForAll @IntRange(min = 2, max = 4) int branchCount) {

        WorkflowGraph graph = buildAllSkippedConvergenceGraph(branchCount);
        Execution execution = createAndStartExecution(graph);

        execution.advance("condition", NodeExecutionResult.routing("selected_target", Map.of()));

        Map<String, ExecutionStatus> nodeStatuses = execution.getNodeStatuses();

        // END 节点应保持 PENDING（因为 selected_target 还是 PENDING，
        // 虽然 convergence 被 SKIPPED 了，但 selected_target → end 这条路径还在）
        assert nodeStatuses.get("end") == ExecutionStatus.PENDING :
                String.format("End node should remain PENDING (selected_target is still PENDING), " +
                        "but was %s", nodeStatuses.get("end"));
    }

    // ========== 辅助格式化方法 ==========

    private String getBranchStatuses(Map<String, ExecutionStatus> nodeStatuses, int count) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            sb.append("branch_").append(i).append("=").append(nodeStatuses.get("branch_" + i));
        }
        sb.append(", convergence=").append(nodeStatuses.get("convergence"));
        sb.append("}");
        return sb.toString();
    }
}
