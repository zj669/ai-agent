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
import java.util.stream.Collectors;

/**
 * Property-Based Test: 直接后继剪枝
 *
 * // Feature: condition-branch-refactor, Property 7: Direct successor pruning
 * **Validates: Requirements 4.1**
 *
 * 验证：
 * 对于任意工作流图，其中条件节点有 N 个直接后继且选中了一个分支，
 * Pruning_Engine 应将恰好 N-1 个直接后继（非选中目标）标记为 SKIPPED。
 *
 * 测试策略：
 * - 生成随机数量的后继节点（2-6 个）
 * - 随机选择一个作为"选中分支"
 * - 调用 advance() 触发剪枝
 * - 验证：恰好 N-1 个非选中后继为 SKIPPED
 * - 验证：选中后继保持 PENDING
 */
class DirectSuccessorPruningPropertyTest {

    // ========== 辅助方法 ==========

    /**
     * 构建一个包含 START → CONDITION → N 个后继 → END 的工作流图。
     *
     * 结构：
     *   start → condition → successor_0
     *                     → successor_1
     *                     → ...
     *                     → successor_{N-1}
     *                     → end (每个 successor 都连到 end)
     *
     * @param successorCount 后继节点数量
     * @return 构建好的 WorkflowGraph
     */
    private WorkflowGraph buildGraph(int successorCount) {
        Map<String, Node> nodes = new HashMap<>();
        Map<String, Set<String>> edges = new HashMap<>();

        // START 节点
        nodes.put("start", Node.builder()
                .nodeId("start")
                .name("Start")
                .type(NodeType.START)
                .config(NodeConfig.builder().build())
                .build());

        // CONDITION 节点
        nodes.put("condition", Node.builder()
                .nodeId("condition")
                .name("Condition")
                .type(NodeType.CONDITION)
                .config(NodeConfig.builder().build())
                .build());

        // END 节点
        nodes.put("end", Node.builder()
                .nodeId("end")
                .name("End")
                .type(NodeType.END)
                .config(NodeConfig.builder().build())
                .build());

        // start → condition
        edges.put("start", new HashSet<>(Set.of("condition")));

        // condition → successor_0, successor_1, ..., successor_{N-1}
        Set<String> conditionSuccessors = new HashSet<>();
        for (int i = 0; i < successorCount; i++) {
            String successorId = "successor_" + i;
            nodes.put(successorId, Node.builder()
                    .nodeId(successorId)
                    .name("Successor " + i)
                    .type(NodeType.LLM)
                    .config(NodeConfig.builder().build())
                    .build());
            conditionSuccessors.add(successorId);

            // successor_i → end
            edges.put(successorId, new HashSet<>(Set.of("end")));
        }
        edges.put("condition", conditionSuccessors);

        return WorkflowGraph.builder()
                .graphId("test-graph")
                .version("1.0")
                .nodes(nodes)
                .edges(edges)
                .build();
    }

    /**
     * 创建 Execution 并启动（初始化所有节点状态为 PENDING）。
     * 然后将 start 节点标记为 SUCCEEDED，condition 节点通过 advance() 触发剪枝。
     */
    private Execution createAndStartExecution(WorkflowGraph graph) {
        Execution execution = Execution.builder()
                .executionId("test-exec-" + UUID.randomUUID())
                .graph(graph)
                .context(ExecutionContext.builder().build())
                .build();

        // 启动执行，初始化所有节点状态为 PENDING
        execution.start(new HashMap<>());

        // 推进 start 节点完成
        execution.advance("start", NodeExecutionResult.success(Map.of()));

        return execution;
    }

    // ========== Property Tests ==========

    // Feature: condition-branch-refactor, Property 7: Direct successor pruning
    // **Validates: Requirements 4.1**
    @Property(tries = 100)
    void exactly_N_minus_1_non_selected_successors_are_skipped(
            @ForAll @IntRange(min = 2, max = 6) int successorCount,
            @ForAll Random random) {

        // 随机选择一个后继作为选中分支
        int selectedIndex = random.nextInt(successorCount);
        String selectedBranchId = "successor_" + selectedIndex;

        WorkflowGraph graph = buildGraph(successorCount);
        Execution execution = createAndStartExecution(graph);

        // 调用 advance() 触发条件节点的剪枝逻辑
        execution.advance("condition", NodeExecutionResult.routing(selectedBranchId, Map.of()));

        // 验证：恰好 N-1 个非选中后继为 SKIPPED
        Map<String, ExecutionStatus> nodeStatuses = execution.getNodeStatuses();

        long skippedCount = 0;
        for (int i = 0; i < successorCount; i++) {
            String successorId = "successor_" + i;
            if (i != selectedIndex) {
                skippedCount++;
                assert nodeStatuses.get(successorId) == ExecutionStatus.SKIPPED :
                        String.format("Non-selected successor '%s' should be SKIPPED, but was %s. " +
                                        "successorCount=%d, selectedBranch=%s",
                                successorId, nodeStatuses.get(successorId),
                                successorCount, selectedBranchId);
            }
        }

        assert skippedCount == successorCount - 1 :
                String.format("Expected %d SKIPPED successors, but found %d. successorCount=%d",
                        successorCount - 1, skippedCount, successorCount);
    }

    // Feature: condition-branch-refactor, Property 7: Direct successor pruning
    // **Validates: Requirements 4.1**
    @Property(tries = 100)
    void selected_successor_remains_pending(
            @ForAll @IntRange(min = 2, max = 6) int successorCount,
            @ForAll Random random) {

        int selectedIndex = random.nextInt(successorCount);
        String selectedBranchId = "successor_" + selectedIndex;

        WorkflowGraph graph = buildGraph(successorCount);
        Execution execution = createAndStartExecution(graph);

        execution.advance("condition", NodeExecutionResult.routing(selectedBranchId, Map.of()));

        Map<String, ExecutionStatus> nodeStatuses = execution.getNodeStatuses();

        // 选中的后继应保持 PENDING（等待被调度执行）
        assert nodeStatuses.get(selectedBranchId) == ExecutionStatus.PENDING :
                String.format("Selected successor '%s' should remain PENDING, but was %s. " +
                                "successorCount=%d",
                        selectedBranchId, nodeStatuses.get(selectedBranchId),
                        successorCount);
    }

    // Feature: condition-branch-refactor, Property 7: Direct successor pruning
    // **Validates: Requirements 4.1**
    @Property(tries = 100)
    void skipped_count_equals_total_successors_minus_one(
            @ForAll @IntRange(min = 2, max = 6) int successorCount,
            @ForAll Random random) {

        int selectedIndex = random.nextInt(successorCount);
        String selectedBranchId = "successor_" + selectedIndex;

        WorkflowGraph graph = buildGraph(successorCount);
        Execution execution = createAndStartExecution(graph);

        execution.advance("condition", NodeExecutionResult.routing(selectedBranchId, Map.of()));

        Map<String, ExecutionStatus> nodeStatuses = execution.getNodeStatuses();

        // 统计所有直接后继中 SKIPPED 的数量
        long skippedSuccessors = 0;
        for (int i = 0; i < successorCount; i++) {
            if (nodeStatuses.get("successor_" + i) == ExecutionStatus.SKIPPED) {
                skippedSuccessors++;
            }
        }

        assert skippedSuccessors == successorCount - 1 :
                String.format("Expected exactly %d SKIPPED direct successors, got %d. " +
                                "successorCount=%d, selectedBranch=%s, statuses=%s",
                        successorCount - 1, skippedSuccessors,
                        successorCount, selectedBranchId,
                        getSuccessorStatuses(nodeStatuses, successorCount));
    }

    // Feature: condition-branch-refactor, Property 7: Direct successor pruning
    // **Validates: Requirements 4.1**
    @Property(tries = 100)
    void condition_node_itself_is_succeeded_after_advance(
            @ForAll @IntRange(min = 2, max = 6) int successorCount,
            @ForAll Random random) {

        int selectedIndex = random.nextInt(successorCount);
        String selectedBranchId = "successor_" + selectedIndex;

        WorkflowGraph graph = buildGraph(successorCount);
        Execution execution = createAndStartExecution(graph);

        execution.advance("condition", NodeExecutionResult.routing(selectedBranchId, Map.of()));

        Map<String, ExecutionStatus> nodeStatuses = execution.getNodeStatuses();

        // 条件节点本身应为 SUCCEEDED（routing 结果的 status 是 SUCCEEDED）
        assert nodeStatuses.get("condition") == ExecutionStatus.SUCCEEDED :
                String.format("Condition node should be SUCCEEDED after advance, but was %s",
                        nodeStatuses.get("condition"));
    }

    // Feature: condition-branch-refactor, Property 7: Direct successor pruning
    // **Validates: Requirements 4.1**
    @Property(tries = 100)
    void any_selected_branch_produces_correct_pruning(
            @ForAll @IntRange(min = 2, max = 6) int successorCount) {

        // 对每个可能的选中分支都验证剪枝正确性
        for (int selectedIndex = 0; selectedIndex < successorCount; selectedIndex++) {
            String selectedBranchId = "successor_" + selectedIndex;

            WorkflowGraph graph = buildGraph(successorCount);
            Execution execution = createAndStartExecution(graph);

            execution.advance("condition", NodeExecutionResult.routing(selectedBranchId, Map.of()));

            Map<String, ExecutionStatus> nodeStatuses = execution.getNodeStatuses();

            for (int i = 0; i < successorCount; i++) {
                String successorId = "successor_" + i;
                ExecutionStatus expected = (i == selectedIndex) ? ExecutionStatus.PENDING : ExecutionStatus.SKIPPED;
                ExecutionStatus actual = nodeStatuses.get(successorId);

                assert actual == expected :
                        String.format("successor_%d: expected %s, got %s. " +
                                        "successorCount=%d, selectedBranch=%s",
                                i, expected, actual, successorCount, selectedBranchId);
            }
        }
    }

    // ========== 辅助格式化方法 ==========

    private String getSuccessorStatuses(Map<String, ExecutionStatus> nodeStatuses, int count) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            sb.append("successor_").append(i).append("=").append(nodeStatuses.get("successor_" + i));
        }
        sb.append("}");
        return sb.toString();
    }
}
