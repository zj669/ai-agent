package com.zj.aiagent.infrastructure.workflow.pruning;

import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Execution;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.ExecutionStatus;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 剪枝边界情况单元测试
 *
 * 测试场景：
 * 1. 菱形 DAG（分支后汇聚）
 * 2. 多层嵌套条件分支
 * 3. 条件节点只有一个后继的情况
 *
 * _Requirements: 4.1, 4.2, 4.3_
 */
class PruningEdgeCaseTest {

    // ========== 辅助方法 ==========

    private Node buildNode(String nodeId, String name, NodeType type) {
        return Node.builder()
                .nodeId(nodeId)
                .name(name)
                .type(type)
                .config(NodeConfig.builder().build())
                .build();
    }

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

    // ========== 测试场景 1: 菱形 DAG（分支后汇聚） ==========

    @Nested
    @DisplayName("菱形 DAG（分支后汇聚）")
    class DiamondDagTests {

        /**
         * 构建菱形 DAG:
         *   START → CONDITION → A → MERGE → END
         *                     → B ↗
         */
        private WorkflowGraph buildDiamondDag() {
            Map<String, Node> nodes = new HashMap<>();
            Map<String, Set<String>> edges = new HashMap<>();

            nodes.put("start", buildNode("start", "Start", NodeType.START));
            nodes.put("condition", buildNode("condition", "Condition", NodeType.CONDITION));
            nodes.put("A", buildNode("A", "Branch A", NodeType.LLM));
            nodes.put("B", buildNode("B", "Branch B", NodeType.LLM));
            nodes.put("merge", buildNode("merge", "Merge", NodeType.LLM));
            nodes.put("end", buildNode("end", "End", NodeType.END));

            edges.put("start", new HashSet<>(Set.of("condition")));
            edges.put("condition", new HashSet<>(Set.of("A", "B")));
            edges.put("A", new HashSet<>(Set.of("merge")));
            edges.put("B", new HashSet<>(Set.of("merge")));
            edges.put("merge", new HashSet<>(Set.of("end")));

            return WorkflowGraph.builder()
                    .graphId("diamond-dag")
                    .version("1.0")
                    .nodes(nodes)
                    .edges(edges)
                    .build();
        }

        @Test
        @DisplayName("选中 A 时，B 被 SKIPPED，MERGE 保持 PENDING（有 PENDING 前驱 A）")
        void should_skip_B_and_keep_merge_pending_when_A_is_selected() {
            WorkflowGraph graph = buildDiamondDag();
            Execution execution = createAndStartExecution(graph);

            // 条件节点选中 A
            execution.advance("condition", NodeExecutionResult.routing("A", Map.of()));

            Map<String, ExecutionStatus> statuses = execution.getNodeStatuses();

            // A 保持 PENDING（等待被调度执行）
            assertEquals(ExecutionStatus.PENDING, statuses.get("A"),
                    "选中的分支 A 应保持 PENDING");

            // B 被 SKIPPED
            assertEquals(ExecutionStatus.SKIPPED, statuses.get("B"),
                    "未选中的分支 B 应被 SKIPPED");

            // MERGE 是汇聚节点（前驱 A=PENDING, B=SKIPPED），应保持 PENDING
            assertEquals(ExecutionStatus.PENDING, statuses.get("merge"),
                    "汇聚节点 MERGE 应保持 PENDING，因为前驱 A 仍为 PENDING");

            // END 保持 PENDING
            assertEquals(ExecutionStatus.PENDING, statuses.get("end"),
                    "END 节点应保持 PENDING");
        }

        @Test
        @DisplayName("选中 B 时，A 被 SKIPPED，MERGE 保持 PENDING（有 PENDING 前驱 B）")
        void should_skip_A_and_keep_merge_pending_when_B_is_selected() {
            WorkflowGraph graph = buildDiamondDag();
            Execution execution = createAndStartExecution(graph);

            // 条件节点选中 B
            execution.advance("condition", NodeExecutionResult.routing("B", Map.of()));

            Map<String, ExecutionStatus> statuses = execution.getNodeStatuses();

            // B 保持 PENDING
            assertEquals(ExecutionStatus.PENDING, statuses.get("B"),
                    "选中的分支 B 应保持 PENDING");

            // A 被 SKIPPED
            assertEquals(ExecutionStatus.SKIPPED, statuses.get("A"),
                    "未选中的分支 A 应被 SKIPPED");

            // MERGE 保持 PENDING
            assertEquals(ExecutionStatus.PENDING, statuses.get("merge"),
                    "汇聚节点 MERGE 应保持 PENDING，因为前驱 B 仍为 PENDING");
        }

        @Test
        @DisplayName("菱形 DAG 中选中 A 后，A 完成，MERGE 变为就绪")
        void should_make_merge_ready_when_selected_branch_completes() {
            WorkflowGraph graph = buildDiamondDag();
            Execution execution = createAndStartExecution(graph);

            // 条件节点选中 A
            execution.advance("condition", NodeExecutionResult.routing("A", Map.of()));

            // A 执行完成
            List<Node> readyNodes = execution.advance("A", NodeExecutionResult.success(Map.of()));

            Map<String, ExecutionStatus> statuses = execution.getNodeStatuses();

            // A 变为 SUCCEEDED
            assertEquals(ExecutionStatus.SUCCEEDED, statuses.get("A"),
                    "分支 A 完成后应为 SUCCEEDED");

            // MERGE 应在就绪节点列表中（前驱 A=SUCCEEDED, B=SKIPPED，有效入度为 0）
            assertTrue(readyNodes.stream().anyMatch(n -> n.getNodeId().equals("merge")),
                    "MERGE 应在就绪节点列表中，因为所有前驱都已完成或跳过");
        }
    }

    // ========== 测试场景 2: 多层嵌套条件分支 ==========

    @Nested
    @DisplayName("多层嵌套条件分支")
    class NestedConditionTests {

        /**
         * 构建多层嵌套条件分支:
         *   START → COND1 → COND2 → X → END
         *                  → Y → END
         *          → Z → END
         */
        private WorkflowGraph buildNestedConditionDag() {
            Map<String, Node> nodes = new HashMap<>();
            Map<String, Set<String>> edges = new HashMap<>();

            nodes.put("start", buildNode("start", "Start", NodeType.START));
            nodes.put("cond1", buildNode("cond1", "Condition 1", NodeType.CONDITION));
            nodes.put("cond2", buildNode("cond2", "Condition 2", NodeType.CONDITION));
            nodes.put("X", buildNode("X", "Node X", NodeType.LLM));
            nodes.put("Y", buildNode("Y", "Node Y", NodeType.LLM));
            nodes.put("Z", buildNode("Z", "Node Z", NodeType.LLM));
            nodes.put("end", buildNode("end", "End", NodeType.END));

            edges.put("start", new HashSet<>(Set.of("cond1")));
            edges.put("cond1", new HashSet<>(Set.of("cond2", "Z")));
            edges.put("cond2", new HashSet<>(Set.of("X", "Y")));
            edges.put("X", new HashSet<>(Set.of("end")));
            edges.put("Y", new HashSet<>(Set.of("end")));
            edges.put("Z", new HashSet<>(Set.of("end")));

            return WorkflowGraph.builder()
                    .graphId("nested-condition-dag")
                    .version("1.0")
                    .nodes(nodes)
                    .edges(edges)
                    .build();
        }

        @Test
        @DisplayName("COND1 选中 COND2，COND2 选中 X → Y 被 SKIPPED，Z 被 SKIPPED")
        void should_skip_Y_and_Z_when_cond1_selects_cond2_and_cond2_selects_X() {
            WorkflowGraph graph = buildNestedConditionDag();
            Execution execution = createAndStartExecution(graph);

            // COND1 选中 COND2（Z 被剪枝）
            execution.advance("cond1", NodeExecutionResult.routing("cond2", Map.of()));

            Map<String, ExecutionStatus> statuses = execution.getNodeStatuses();

            // Z 被 SKIPPED（COND1 的未选中分支）
            assertEquals(ExecutionStatus.SKIPPED, statuses.get("Z"),
                    "COND1 未选中的分支 Z 应被 SKIPPED");

            // COND2 保持 PENDING
            assertEquals(ExecutionStatus.PENDING, statuses.get("cond2"),
                    "选中的分支 COND2 应保持 PENDING");

            // COND2 选中 X（Y 被剪枝）
            execution.advance("cond2", NodeExecutionResult.routing("X", Map.of()));

            statuses = execution.getNodeStatuses();

            // Y 被 SKIPPED（COND2 的未选中分支）
            assertEquals(ExecutionStatus.SKIPPED, statuses.get("Y"),
                    "COND2 未选中的分支 Y 应被 SKIPPED");

            // X 保持 PENDING
            assertEquals(ExecutionStatus.PENDING, statuses.get("X"),
                    "选中的分支 X 应保持 PENDING");

            // Z 仍然是 SKIPPED
            assertEquals(ExecutionStatus.SKIPPED, statuses.get("Z"),
                    "Z 应仍然保持 SKIPPED");
        }

        @Test
        @DisplayName("COND1 选中 Z → COND2、X、Y 都被 SKIPPED")
        void should_skip_cond2_X_Y_when_cond1_selects_Z() {
            WorkflowGraph graph = buildNestedConditionDag();
            Execution execution = createAndStartExecution(graph);

            // COND1 选中 Z（COND2 被剪枝，COND2 的下游 X、Y 也应被递归跳过）
            execution.advance("cond1", NodeExecutionResult.routing("Z", Map.of()));

            Map<String, ExecutionStatus> statuses = execution.getNodeStatuses();

            // Z 保持 PENDING
            assertEquals(ExecutionStatus.PENDING, statuses.get("Z"),
                    "选中的分支 Z 应保持 PENDING");

            // COND2 被 SKIPPED
            assertEquals(ExecutionStatus.SKIPPED, statuses.get("cond2"),
                    "未选中的分支 COND2 应被 SKIPPED");

            // X 被递归 SKIPPED（COND2 的下游，单前驱）
            assertEquals(ExecutionStatus.SKIPPED, statuses.get("X"),
                    "COND2 的下游 X 应被递归 SKIPPED");

            // Y 被递归 SKIPPED（COND2 的下游，单前驱）
            assertEquals(ExecutionStatus.SKIPPED, statuses.get("Y"),
                    "COND2 的下游 Y 应被递归 SKIPPED");
        }

        @Test
        @DisplayName("嵌套条件分支完整执行路径: COND1→COND2→X→END")
        void should_complete_execution_through_nested_conditions() {
            WorkflowGraph graph = buildNestedConditionDag();
            Execution execution = createAndStartExecution(graph);

            // COND1 选中 COND2
            execution.advance("cond1", NodeExecutionResult.routing("cond2", Map.of()));

            // COND2 选中 X
            execution.advance("cond2", NodeExecutionResult.routing("X", Map.of()));

            // X 执行完成
            List<Node> readyNodes = execution.advance("X", NodeExecutionResult.success(Map.of()));

            Map<String, ExecutionStatus> statuses = execution.getNodeStatuses();

            // END 应在就绪节点列表中（前驱 X=SUCCEEDED, Y=SKIPPED, Z=SKIPPED）
            assertTrue(readyNodes.stream().anyMatch(n -> n.getNodeId().equals("end")),
                    "END 应在就绪节点列表中");
        }
    }

    // ========== 测试场景 3: 条件节点只有一个后继 ==========

    @Nested
    @DisplayName("条件节点只有一个后继")
    class SingleSuccessorTests {

        /**
         * 构建条件节点只有一个后继的 DAG:
         *   START → CONDITION → ONLY_SUCCESSOR → END
         */
        private WorkflowGraph buildSingleSuccessorDag() {
            Map<String, Node> nodes = new HashMap<>();
            Map<String, Set<String>> edges = new HashMap<>();

            nodes.put("start", buildNode("start", "Start", NodeType.START));
            nodes.put("condition", buildNode("condition", "Condition", NodeType.CONDITION));
            nodes.put("only_successor", buildNode("only_successor", "Only Successor", NodeType.LLM));
            nodes.put("end", buildNode("end", "End", NodeType.END));

            edges.put("start", new HashSet<>(Set.of("condition")));
            edges.put("condition", new HashSet<>(Set.of("only_successor")));
            edges.put("only_successor", new HashSet<>(Set.of("end")));

            return WorkflowGraph.builder()
                    .graphId("single-successor-dag")
                    .version("1.0")
                    .nodes(nodes)
                    .edges(edges)
                    .build();
        }

        @Test
        @DisplayName("选中唯一后继 → 无剪枝，ONLY_SUCCESSOR 保持 PENDING")
        void should_not_prune_when_only_one_successor_is_selected() {
            WorkflowGraph graph = buildSingleSuccessorDag();
            Execution execution = createAndStartExecution(graph);

            // 条件节点选中唯一后继
            execution.advance("condition", NodeExecutionResult.routing("only_successor", Map.of()));

            Map<String, ExecutionStatus> statuses = execution.getNodeStatuses();

            // ONLY_SUCCESSOR 保持 PENDING（被选中，不应被剪枝）
            assertEquals(ExecutionStatus.PENDING, statuses.get("only_successor"),
                    "唯一后继 ONLY_SUCCESSOR 应保持 PENDING");

            // END 保持 PENDING
            assertEquals(ExecutionStatus.PENDING, statuses.get("end"),
                    "END 节点应保持 PENDING");

            // 条件节点本身为 SUCCEEDED
            assertEquals(ExecutionStatus.SUCCEEDED, statuses.get("condition"),
                    "条件节点应为 SUCCEEDED");
        }

        @Test
        @DisplayName("选中唯一后继后完成执行 → 正常流转到 END")
        void should_flow_to_end_after_only_successor_completes() {
            WorkflowGraph graph = buildSingleSuccessorDag();
            Execution execution = createAndStartExecution(graph);

            // 条件节点选中唯一后继
            execution.advance("condition", NodeExecutionResult.routing("only_successor", Map.of()));

            // ONLY_SUCCESSOR 执行完成
            List<Node> readyNodes = execution.advance("only_successor", NodeExecutionResult.success(Map.of()));

            // END 应在就绪节点列表中
            assertTrue(readyNodes.stream().anyMatch(n -> n.getNodeId().equals("end")),
                    "END 应在就绪节点列表中");

            Map<String, ExecutionStatus> statuses = execution.getNodeStatuses();
            assertEquals(ExecutionStatus.SUCCEEDED, statuses.get("only_successor"),
                    "ONLY_SUCCESSOR 完成后应为 SUCCEEDED");
        }
    }
}
