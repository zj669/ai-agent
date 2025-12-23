package com.zj.aiagent.service.dag;

import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.entity.GraphJsonSchema;
import com.zj.aiagent.domain.agent.dag.executor.DagDependencyTracker;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DagDependencyTracker 单元测试
 */
class DagDependencyTrackerTest {

    /**
     * 测试简单的线性DAG
     * A -> B -> C
     */
    @Test
    void testLinearDag() {
        DagGraph dag = createLinearDag();
        DagDependencyTracker tracker = new DagDependencyTracker(dag);

        // 初始状态：只有 A 就绪
        Set<String> readyNodes = tracker.getReadyNodes();
        assertEquals(1, readyNodes.size());
        assertTrue(readyNodes.contains("A"));

        // 标记 A 完成
        Set<String> newReady = tracker.markCompleted("A");
        assertEquals(1, newReady.size());
        assertTrue(newReady.contains("B"));

        // 标记 B 完成
        newReady = tracker.markCompleted("B");
        assertEquals(1, newReady.size());
        assertTrue(newReady.contains("C"));

        // 标记 C 完成
        newReady = tracker.markCompleted("C");
        assertTrue(newReady.isEmpty());

        // 所有节点完成
        assertTrue(tracker.isAllCompleted());
    }

    /**
     * 测试菱形DAG（核心测试用例）
     * A
     * / \
     * B C
     * \ /
     * D
     */
    @Test
    void testDiamondDag() {
        DagGraph dag = createDiamondDag();
        DagDependencyTracker tracker = new DagDependencyTracker(dag);

        // 初始状态：只有 A 就绪
        Set<String> readyNodes = tracker.getReadyNodes();
        assertEquals(1, readyNodes.size());
        assertTrue(readyNodes.contains("A"));

        // 标记 A 完成，B 和 C 应该都就绪
        Set<String> newReady = tracker.markCompleted("A");
        assertEquals(2, newReady.size());
        assertTrue(newReady.contains("B"));
        assertTrue(newReady.contains("C"));

        // 标记 B 完成，D 还不能就绪（因为还依赖 C）
        newReady = tracker.markCompleted("B");
        assertTrue(newReady.isEmpty());
        assertEquals(1, tracker.getPendingDependencies("D")); // D 还有1个依赖

        // 标记 C 完成，D 应该就绪
        newReady = tracker.markCompleted("C");
        assertEquals(1, newReady.size());
        assertTrue(newReady.contains("D"));

        // 标记 D 完成
        newReady = tracker.markCompleted("D");
        assertTrue(newReady.isEmpty());

        // 所有节点完成
        assertTrue(tracker.isAllCompleted());
        assertEquals(4, tracker.getCompletedCount());
    }

    /**
     * 测试路由节点禁用功能
     * A
     * / \
     * B C
     * | |
     * D E
     */
    @Test
    void testRouterNodeDisabling() {
        DagGraph dag = createRouterDag();
        DagDependencyTracker tracker = new DagDependencyTracker(dag);

        // A 完成后，B 和 C 都就绪
        tracker.markCompleted("A");

        // 假设路由选择了 B 分支，禁用 C 及其下游
        tracker.disableNode("C");

        // 验证启用节点数（A, B, D 启用，C, E 禁用）
        assertEquals(3, tracker.getEnabledCount());

        // B 完成后，D 应该就绪
        Set<String> newReady = tracker.markCompleted("B");
        assertTrue(newReady.contains("D"));

        // 完成 D
        tracker.markCompleted("D");

        // 所有启用节点都完成
        assertTrue(tracker.isAllCompleted());
        assertEquals(3, tracker.getCompletedCount()); // A, B, D
    }

    /**
     * 测试复杂DAG
     * A
     * /|\
     * B C D
     * |X| |
     * E F G
     * \|/
     * H
     */
    @Test
    void testComplexDag() {
        DagGraph dag = createComplexDag();
        DagDependencyTracker tracker = new DagDependencyTracker(dag);

        // A 完成后，B, C, D 都就绪
        Set<String> newReady = tracker.markCompleted("A");
        assertEquals(3, newReady.size());

        // 完成 B，E 就绪
        newReady = tracker.markCompleted("B");
        assertTrue(newReady.contains("E"));

        // 完成 C，F 就绪
        newReady = tracker.markCompleted("C");
        assertTrue(newReady.contains("F"));

        // 完成 D，G 就绪
        newReady = tracker.markCompleted("D");
        assertTrue(newReady.contains("G"));

        // 完成 E, F, G 中的任意两个，H 不应该就绪
        tracker.markCompleted("E");
        tracker.markCompleted("F");

        // 完成 G，H 就绪
        newReady = tracker.markCompleted("G");
        assertTrue(newReady.contains("H"));

        tracker.markCompleted("H");
        assertTrue(tracker.isAllCompleted());
    }

    // ========== 辅助方法：创建测试用DAG ==========

    private DagGraph createLinearDag() {
        Map<String, com.zj.aiagent.domain.agent.dag.node.AbstractConfigurableNode> nodes = new HashMap<>();
        // 简化：这里只需要节点ID，不需要完整的节点对象

        List<GraphJsonSchema.EdgeDefinition> edges = Arrays.asList(
                new GraphJsonSchema.EdgeDefinition("A", "B"),
                new GraphJsonSchema.EdgeDefinition("B", "C"));

        return DagGraph.builder()
                .dagId("linear-dag")
                .nodes(createMockNodes(Arrays.asList("A", "B", "C")))
                .edges(edges)
                .startNodeId("A")
                .build();
    }

    private DagGraph createDiamondDag() {
        List<GraphJsonSchema.EdgeDefinition> edges = Arrays.asList(
                new GraphJsonSchema.EdgeDefinition("A", "B"),
                new GraphJsonSchema.EdgeDefinition("A", "C"),
                new GraphJsonSchema.EdgeDefinition("B", "D"),
                new GraphJsonSchema.EdgeDefinition("C", "D"));

        return DagGraph.builder()
                .dagId("diamond-dag")
                .nodes(createMockNodes(Arrays.asList("A", "B", "C", "D")))
                .edges(edges)
                .startNodeId("A")
                .build();
    }

    private DagGraph createRouterDag() {
        List<GraphJsonSchema.EdgeDefinition> edges = Arrays.asList(
                new GraphJsonSchema.EdgeDefinition("A", "B"),
                new GraphJsonSchema.EdgeDefinition("A", "C"),
                new GraphJsonSchema.EdgeDefinition("B", "D"),
                new GraphJsonSchema.EdgeDefinition("C", "E"));

        return DagGraph.builder()
                .dagId("router-dag")
                .nodes(createMockNodes(Arrays.asList("A", "B", "C", "D", "E")))
                .edges(edges)
                .startNodeId("A")
                .build();
    }

    private DagGraph createComplexDag() {
        List<GraphJsonSchema.EdgeDefinition> edges = Arrays.asList(
                new GraphJsonSchema.EdgeDefinition("A", "B"),
                new GraphJsonSchema.EdgeDefinition("A", "C"),
                new GraphJsonSchema.EdgeDefinition("A", "D"),
                new GraphJsonSchema.EdgeDefinition("B", "E"),
                new GraphJsonSchema.EdgeDefinition("C", "F"),
                new GraphJsonSchema.EdgeDefinition("D", "G"),
                new GraphJsonSchema.EdgeDefinition("E", "H"),
                new GraphJsonSchema.EdgeDefinition("F", "H"),
                new GraphJsonSchema.EdgeDefinition("G", "H"));

        return DagGraph.builder()
                .dagId("complex-dag")
                .nodes(createMockNodes(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H")))
                .edges(edges)
                .startNodeId("A")
                .build();
    }

    /**
     * 创建模拟节点（不需要实际功能，只需要ID）
     */
    private Map<String, com.zj.aiagent.domain.agent.dag.node.AbstractConfigurableNode> createMockNodes(
            List<String> nodeIds) {
        Map<String, com.zj.aiagent.domain.agent.dag.node.AbstractConfigurableNode> nodes = new HashMap<>();
        // 这里返回 null，因为 DagDependencyTracker 只需要节点ID
        // 实际使用时应该创建 Mock 对象
        for (String nodeId : nodeIds) {
            nodes.put(nodeId, null);
        }
        return nodes;
    }
}
