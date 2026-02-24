package com.zj.aiagent.infrastructure.workflow.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.entity.Edge;
import com.zj.aiagent.domain.workflow.valobj.ComparisonOperator;
import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.ConditionGroup;
import com.zj.aiagent.domain.workflow.valobj.ConditionItem;
import com.zj.aiagent.infrastructure.workflow.graph.converter.NodeConfigConverter;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Property-Based Test: 旧模型 Edge → ConditionBranch 转换
 *
 * // Feature: condition-branch-refactor, Property 10: Legacy edge to branch conversion
 * **Validates: Requirements 9.1, 9.2**
 *
 * 验证：
 * - DEFAULT 边始终转换为 isDefault=true 的分支
 * - CONDITIONAL 边（可解析 SpEL）转换为非 default 分支，operator 映射正确
 * - 不可解析的 SpEL 降级为 default 分支
 * - 转换后的分支数量 = 原始边数量
 * - 非 default 分支的 priority 单调递增
 */
class LegacyEdgeToBranchConversionPropertyTest {

    private final WorkflowGraphFactoryImpl factory;

    LegacyEdgeToBranchConversionPropertyTest() {
        ObjectMapper objectMapper = new ObjectMapper();
        NodeConfigConverter mockConverter = mock(NodeConfigConverter.class);
        this.factory = new WorkflowGraphFactoryImpl(objectMapper, mockConverter);
    }

    // ========== SpEL 表达式模板 ==========

    /** 可解析的简单比较 SpEL */
    private static final List<String> PARSEABLE_SPELS = List.of(
            "#input == 100",
            "#score > 80",
            "#count < 10",
            "#age >= 18",
            "#price <= 999",
            "#name != 'test'",
            "#value == 'hello'",
            "#flag == true",
            "#amount > 0"
    );

    /** 不可解析的复杂 SpEL */
    private static final List<String> UNPARSEABLE_SPELS = List.of(
            "#a > 100 && #b < 200",
            "#input > 0 ? 'yes' : 'no'",
            "#list.size() > 0",
            "#map['key'] == 'value'",
            "T(Math).random() > 0.5"
    );

    // ========== Generators ==========

    @Provide
    Arbitrary<List<Edge>> mixedEdges() {
        Arbitrary<Edge> conditionalEdge = Arbitraries.of(PARSEABLE_SPELS)
                .map(spel -> Edge.builder()
                        .edgeId("edge_" + UUID.randomUUID().toString().substring(0, 8))
                        .source("condition_node")
                        .target("target_" + UUID.randomUUID().toString().substring(0, 8))
                        .condition(spel)
                        .edgeType(Edge.EdgeType.CONDITIONAL)
                        .build());

        Arbitrary<Edge> defaultEdge = Arbitraries.just(
                Edge.builder()
                        .edgeId("edge_default")
                        .source("condition_node")
                        .target("target_default")
                        .condition(null)
                        .edgeType(Edge.EdgeType.DEFAULT)
                        .build());

        // 1~5 个 conditional 边 + 1 个 default 边
        return conditionalEdge.list().ofMinSize(1).ofMaxSize(5)
                .map(list -> {
                    List<Edge> edges = new ArrayList<>(list);
                    edges.add(Edge.builder()
                            .edgeId("edge_default")
                            .source("condition_node")
                            .target("target_default")
                            .condition(null)
                            .edgeType(Edge.EdgeType.DEFAULT)
                            .build());
                    return edges;
                });
    }

    @Provide
    Arbitrary<List<Edge>> onlyConditionalEdges() {
        return Arbitraries.of(PARSEABLE_SPELS)
                .map(spel -> Edge.builder()
                        .edgeId("edge_" + UUID.randomUUID().toString().substring(0, 8))
                        .source("condition_node")
                        .target("target_" + UUID.randomUUID().toString().substring(0, 8))
                        .condition(spel)
                        .edgeType(Edge.EdgeType.CONDITIONAL)
                        .build())
                .list().ofMinSize(1).ofMaxSize(5);
    }

    // ========== Property Tests ==========

    /**
     * Property: DEFAULT 边始终转换为 isDefault=true 的分支
     * Validates: Requirement 9.2
     */
    @Property(tries = 100)
    void default_edges_always_become_default_branches(
            @ForAll("mixedEdges") List<Edge> edges) {

        List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(edges);

        long defaultEdgeCount = edges.stream()
                .filter(Edge::isDefault)
                .count();
        long defaultBranchCount = branches.stream()
                .filter(ConditionBranch::isDefault)
                .count();

        // 至少有一个 default 分支（来自 DEFAULT 边）
        assertTrue(defaultBranchCount >= defaultEdgeCount,
                String.format("Default branches (%d) should be >= default edges (%d)",
                        defaultBranchCount, defaultEdgeCount));
    }

    /**
     * Property: 可解析的 CONDITIONAL 边转换为非 default 分支
     * Validates: Requirement 9.1
     */
    @Property(tries = 100)
    void parseable_conditional_edges_become_non_default_branches(
            @ForAll("mixedEdges") List<Edge> edges) {

        List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(edges);

        long conditionalEdgeCount = edges.stream()
                .filter(e -> !e.isDefault())
                .count();
        long nonDefaultBranchCount = branches.stream()
                .filter(b -> !b.isDefault())
                .count();

        // 可解析的 conditional 边应该变成非 default 分支
        assertEquals(conditionalEdgeCount, nonDefaultBranchCount,
                "Parseable conditional edges should become non-default branches");
    }

    /**
     * Property: 转换后的分支数量等于原始边数量
     * Validates: Requirements 9.1, 9.2
     */
    @Property(tries = 100)
    void branch_count_equals_edge_count(
            @ForAll("mixedEdges") List<Edge> edges) {

        List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(edges);

        assertEquals(edges.size(), branches.size(),
                "Branch count should equal edge count");
    }

    /**
     * Property: 非 default 分支的 priority 从 0 开始单调递增
     * Validates: Requirement 9.1
     */
    @Property(tries = 100)
    void non_default_branch_priorities_are_monotonically_increasing(
            @ForAll("mixedEdges") List<Edge> edges) {

        List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(edges);

        List<Integer> nonDefaultPriorities = branches.stream()
                .filter(b -> !b.isDefault())
                .map(ConditionBranch::getPriority)
                .collect(Collectors.toList());

        for (int i = 0; i < nonDefaultPriorities.size(); i++) {
            assertEquals(i, nonDefaultPriorities.get(i),
                    "Non-default branch priority should be " + i + " but was " + nonDefaultPriorities.get(i));
        }
    }

    /**
     * Property: 每个非 default 分支都有至少一个 ConditionGroup 和 ConditionItem
     * Validates: Requirement 9.1
     */
    @Property(tries = 100)
    void non_default_branches_have_condition_groups(
            @ForAll("mixedEdges") List<Edge> edges) {

        List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(edges);

        for (ConditionBranch branch : branches) {
            if (!branch.isDefault()) {
                assertNotNull(branch.getConditionGroups(),
                        "Non-default branch should have condition groups");
                assertFalse(branch.getConditionGroups().isEmpty(),
                        "Non-default branch should have at least one condition group");

                for (ConditionGroup group : branch.getConditionGroups()) {
                    assertNotNull(group.getConditions(),
                            "Condition group should have conditions");
                    assertFalse(group.getConditions().isEmpty(),
                            "Condition group should have at least one condition item");

                    for (ConditionItem item : group.getConditions()) {
                        assertNotNull(item.getOperator(),
                                "Condition item should have a non-null operator");
                        assertNotNull(item.getLeftOperand(),
                                "Condition item should have a non-null left operand");
                    }
                }
            }
        }
    }

    /**
     * Property: default 分支的 conditionGroups 为空
     * Validates: Requirement 9.2
     */
    @Property(tries = 100)
    void default_branches_have_empty_condition_groups(
            @ForAll("mixedEdges") List<Edge> edges) {

        List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(edges);

        for (ConditionBranch branch : branches) {
            if (branch.isDefault()) {
                assertTrue(branch.getConditionGroups() == null || branch.getConditionGroups().isEmpty(),
                        "Default branch should have empty condition groups");
                assertEquals(Integer.MAX_VALUE, branch.getPriority(),
                        "Default branch priority should be Integer.MAX_VALUE");
            }
        }
    }

    /**
     * Property: 不可解析的 SpEL 表达式降级为 default 分支
     * Validates: Requirement 9.3
     */
    @Property(tries = 50)
    void unparseable_spel_degrades_to_default(
            @ForAll @IntRange(min = 0, max = 4) int unparseableIndex) {

        String unparseableSpel = UNPARSEABLE_SPELS.get(unparseableIndex);

        Edge unparseableEdge = Edge.builder()
                .edgeId("edge_unparseable")
                .source("condition_node")
                .target("target_unparseable")
                .condition(unparseableSpel)
                .edgeType(Edge.EdgeType.CONDITIONAL)
                .build();

        List<Edge> edges = List.of(unparseableEdge);
        List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(edges);

        assertEquals(1, branches.size());
        assertTrue(branches.get(0).isDefault(),
                "Unparseable SpEL '" + unparseableSpel + "' should degrade to default branch");
        assertEquals("target_unparseable", branches.get(0).getTargetNodeId());
    }

    /**
     * Property: targetNodeId 保持不变
     * Validates: Requirements 9.1, 9.2
     */
    @Property(tries = 100)
    void target_node_ids_are_preserved(
            @ForAll("mixedEdges") List<Edge> edges) {

        List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(edges);

        Set<String> edgeTargets = edges.stream()
                .map(Edge::getTarget)
                .collect(Collectors.toSet());
        Set<String> branchTargets = branches.stream()
                .map(ConditionBranch::getTargetNodeId)
                .collect(Collectors.toSet());

        assertEquals(edgeTargets, branchTargets,
                "Target node IDs should be preserved after conversion");
    }
}
