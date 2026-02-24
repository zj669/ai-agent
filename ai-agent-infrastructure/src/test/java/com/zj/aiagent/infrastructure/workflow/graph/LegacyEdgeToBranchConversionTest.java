package com.zj.aiagent.infrastructure.workflow.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.entity.Edge;
import com.zj.aiagent.domain.workflow.valobj.ComparisonOperator;
import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.ConditionGroup;
import com.zj.aiagent.domain.workflow.valobj.ConditionItem;
import com.zj.aiagent.infrastructure.workflow.graph.converter.NodeConfigConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 旧模型兼容转换单元测试 — 边界情况
 *
 * 覆盖场景：
 * - 典型 SpEL 表达式转换（Requirements 9.1）
 * - 无法解析的复杂 SpEL 表达式降级为 default（Requirements 9.3）
 * - 混合 CONDITIONAL + DEFAULT 边的转换（Requirements 9.1, 9.2）
 * - 方法调用 SpEL 转换（contains, startsWith, endsWith, isEmpty）
 * - SpEL 字面值解析（字符串、数值、布尔值、null）
 */
class LegacyEdgeToBranchConversionTest {

    private WorkflowGraphFactoryImpl factory;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        NodeConfigConverter mockConverter = mock(NodeConfigConverter.class);
        factory = new WorkflowGraphFactoryImpl(objectMapper, mockConverter);
    }

    // ========== parseLegacySpelToItem 测试 ==========

    @Nested
    @DisplayName("SpEL 比较操作符转换")
    class SpelComparisonTests {

        @Test
        @DisplayName("#input > 100 → GREATER_THAN, rightOperand=100L")
        void should_parse_greater_than() {
            ConditionItem item = factory.parseLegacySpelToItem("#input > 100");

            assertNotNull(item);
            assertEquals("inputs.input", item.getLeftOperand());
            assertEquals(ComparisonOperator.GREATER_THAN, item.getOperator());
            assertEquals(100L, item.getRightOperand());
        }

        @Test
        @DisplayName("#score < 60 → LESS_THAN, rightOperand=60L")
        void should_parse_less_than() {
            ConditionItem item = factory.parseLegacySpelToItem("#score < 60");

            assertNotNull(item);
            assertEquals("inputs.score", item.getLeftOperand());
            assertEquals(ComparisonOperator.LESS_THAN, item.getOperator());
            assertEquals(60L, item.getRightOperand());
        }

        @Test
        @DisplayName("#age >= 18 → GREATER_THAN_OR_EQUAL")
        void should_parse_greater_than_or_equal() {
            ConditionItem item = factory.parseLegacySpelToItem("#age >= 18");

            assertNotNull(item);
            assertEquals(ComparisonOperator.GREATER_THAN_OR_EQUAL, item.getOperator());
            assertEquals(18L, item.getRightOperand());
        }

        @Test
        @DisplayName("#price <= 999.99 → LESS_THAN_OR_EQUAL, rightOperand=999.99")
        void should_parse_less_than_or_equal_with_double() {
            ConditionItem item = factory.parseLegacySpelToItem("#price <= 999.99");

            assertNotNull(item);
            assertEquals(ComparisonOperator.LESS_THAN_OR_EQUAL, item.getOperator());
            assertEquals(999.99, item.getRightOperand());
        }

        @Test
        @DisplayName("#name == 'hello' → EQUALS, rightOperand=\"hello\"")
        void should_parse_equals_with_string() {
            ConditionItem item = factory.parseLegacySpelToItem("#name == 'hello'");

            assertNotNull(item);
            assertEquals("inputs.name", item.getLeftOperand());
            assertEquals(ComparisonOperator.EQUALS, item.getOperator());
            assertEquals("hello", item.getRightOperand());
        }

        @Test
        @DisplayName("#status != 'active' → NOT_EQUALS")
        void should_parse_not_equals() {
            ConditionItem item = factory.parseLegacySpelToItem("#status != 'active'");

            assertNotNull(item);
            assertEquals(ComparisonOperator.NOT_EQUALS, item.getOperator());
            assertEquals("active", item.getRightOperand());
        }

        @Test
        @DisplayName("#flag == true → EQUALS, rightOperand=Boolean.TRUE")
        void should_parse_equals_with_boolean() {
            ConditionItem item = factory.parseLegacySpelToItem("#flag == true");

            assertNotNull(item);
            assertEquals(ComparisonOperator.EQUALS, item.getOperator());
            assertEquals(Boolean.TRUE, item.getRightOperand());
        }

        @Test
        @DisplayName("#value == null → EQUALS, rightOperand=null")
        void should_parse_equals_with_null() {
            ConditionItem item = factory.parseLegacySpelToItem("#value == null");

            assertNotNull(item);
            assertEquals(ComparisonOperator.EQUALS, item.getOperator());
            assertNull(item.getRightOperand());
        }
    }

    @Nested
    @DisplayName("SpEL 方法调用转换")
    class SpelMethodTests {

        @Test
        @DisplayName("#text.contains('keyword') → CONTAINS")
        void should_parse_contains() {
            ConditionItem item = factory.parseLegacySpelToItem("#text.contains('keyword')");

            assertNotNull(item);
            assertEquals("inputs.text", item.getLeftOperand());
            assertEquals(ComparisonOperator.CONTAINS, item.getOperator());
            assertEquals("keyword", item.getRightOperand());
        }

        @Test
        @DisplayName("#name.startsWith('prefix') → STARTS_WITH")
        void should_parse_starts_with() {
            ConditionItem item = factory.parseLegacySpelToItem("#name.startsWith('prefix')");

            assertNotNull(item);
            assertEquals(ComparisonOperator.STARTS_WITH, item.getOperator());
            assertEquals("prefix", item.getRightOperand());
        }

        @Test
        @DisplayName("#name.endsWith('suffix') → ENDS_WITH")
        void should_parse_ends_with() {
            ConditionItem item = factory.parseLegacySpelToItem("#name.endsWith('suffix')");

            assertNotNull(item);
            assertEquals(ComparisonOperator.ENDS_WITH, item.getOperator());
            assertEquals("suffix", item.getRightOperand());
        }

        @Test
        @DisplayName("#value.isEmpty() → IS_EMPTY, rightOperand=null")
        void should_parse_is_empty() {
            ConditionItem item = factory.parseLegacySpelToItem("#value.isEmpty()");

            assertNotNull(item);
            assertEquals("inputs.value", item.getLeftOperand());
            assertEquals(ComparisonOperator.IS_EMPTY, item.getOperator());
            // IS_EMPTY 不需要右操作数
        }
    }

    @Nested
    @DisplayName("不可解析的 SpEL 表达式")
    class UnparseableSpelTests {

        @Test
        @DisplayName("复合 AND 表达式返回 null")
        void should_return_null_for_compound_and() {
            assertNull(factory.parseLegacySpelToItem("#a > 100 && #b < 200"));
        }

        @Test
        @DisplayName("复合 OR 表达式返回 null")
        void should_return_null_for_compound_or() {
            assertNull(factory.parseLegacySpelToItem("#a > 100 || #b < 200"));
        }

        @Test
        @DisplayName("三元表达式返回 null")
        void should_return_null_for_ternary() {
            assertNull(factory.parseLegacySpelToItem("#input > 0 ? 'yes' : 'no'"));
        }

        @Test
        @DisplayName("嵌套变量引用返回 null")
        void should_return_null_for_nested_variable() {
            assertNull(factory.parseLegacySpelToItem("#a > #b"));
        }

        @Test
        @DisplayName("null 输入返回 null")
        void should_return_null_for_null_input() {
            assertNull(factory.parseLegacySpelToItem(null));
        }

        @Test
        @DisplayName("空字符串返回 null")
        void should_return_null_for_empty_string() {
            assertNull(factory.parseLegacySpelToItem(""));
        }

        @Test
        @DisplayName("空白字符串返回 null")
        void should_return_null_for_blank_string() {
            assertNull(factory.parseLegacySpelToItem("   "));
        }
    }

    // ========== convertLegacyEdgesToBranches 测试 ==========

    @Nested
    @DisplayName("混合 CONDITIONAL + DEFAULT 边转换")
    class MixedEdgeConversionTests {

        @Test
        @DisplayName("典型场景: 2 个 CONDITIONAL + 1 个 DEFAULT")
        void should_convert_mixed_edges() {
            List<Edge> edges = List.of(
                    Edge.builder()
                            .edgeId("e1").source("cond").target("node_a")
                            .condition("#score > 80")
                            .edgeType(Edge.EdgeType.CONDITIONAL)
                            .build(),
                    Edge.builder()
                            .edgeId("e2").source("cond").target("node_b")
                            .condition("#score <= 80")
                            .edgeType(Edge.EdgeType.CONDITIONAL)
                            .build(),
                    Edge.builder()
                            .edgeId("e3").source("cond").target("node_default")
                            .condition(null)
                            .edgeType(Edge.EdgeType.DEFAULT)
                            .build()
            );

            List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(edges);

            assertEquals(3, branches.size());

            // 非 default 分支
            List<ConditionBranch> nonDefault = branches.stream()
                    .filter(b -> !b.isDefault()).toList();
            assertEquals(2, nonDefault.size());
            assertEquals(0, nonDefault.get(0).getPriority());
            assertEquals(1, nonDefault.get(1).getPriority());
            assertEquals("node_a", nonDefault.get(0).getTargetNodeId());
            assertEquals("node_b", nonDefault.get(1).getTargetNodeId());

            // default 分支
            List<ConditionBranch> defaults = branches.stream()
                    .filter(ConditionBranch::isDefault).toList();
            assertEquals(1, defaults.size());
            assertEquals("node_default", defaults.get(0).getTargetNodeId());
            assertEquals(Integer.MAX_VALUE, defaults.get(0).getPriority());
        }

        @Test
        @DisplayName("不可解析的 CONDITIONAL 边降级为 default")
        void should_degrade_unparseable_conditional_to_default() {
            List<Edge> edges = List.of(
                    Edge.builder()
                            .edgeId("e1").source("cond").target("node_a")
                            .condition("#a > 100 && #b < 200")
                            .edgeType(Edge.EdgeType.CONDITIONAL)
                            .build(),
                    Edge.builder()
                            .edgeId("e2").source("cond").target("node_b")
                            .condition("#score > 80")
                            .edgeType(Edge.EdgeType.CONDITIONAL)
                            .build()
            );

            List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(edges);

            assertEquals(2, branches.size());

            // 第一个不可解析 → default
            assertTrue(branches.get(0).isDefault());
            assertEquals("node_a", branches.get(0).getTargetNodeId());

            // 第二个可解析 → 非 default
            assertFalse(branches.get(1).isDefault());
            assertEquals("node_b", branches.get(1).getTargetNodeId());
            assertEquals(0, branches.get(1).getPriority());
        }

        @Test
        @DisplayName("只有 DEFAULT 边")
        void should_handle_only_default_edge() {
            List<Edge> edges = List.of(
                    Edge.builder()
                            .edgeId("e1").source("cond").target("node_default")
                            .condition(null)
                            .edgeType(Edge.EdgeType.DEFAULT)
                            .build()
            );

            List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(edges);

            assertEquals(1, branches.size());
            assertTrue(branches.get(0).isDefault());
            assertEquals("node_default", branches.get(0).getTargetNodeId());
        }

        @Test
        @DisplayName("condition 为 'default' 字符串的边视为 DEFAULT")
        void should_treat_condition_default_string_as_default_edge() {
            Edge edge = Edge.builder()
                    .edgeId("e1").source("cond").target("node_default")
                    .condition("default")
                    .edgeType(Edge.EdgeType.DEPENDENCY)
                    .build();

            // Edge.isDefault() 会检查 condition == "default"
            assertTrue(edge.isDefault());

            List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(List.of(edge));

            assertEquals(1, branches.size());
            assertTrue(branches.get(0).isDefault());
        }

        @Test
        @DisplayName("空边列表返回空分支列表")
        void should_return_empty_for_empty_edges() {
            List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(List.of());

            assertNotNull(branches);
            assertTrue(branches.isEmpty());
        }
    }

    @Nested
    @DisplayName("操作符映射正确性")
    class OperatorMappingTests {

        @Test
        @DisplayName("所有比较操作符映射正确")
        void should_map_all_comparison_operators() {
            assertOperatorMapping("#x == 1", ComparisonOperator.EQUALS);
            assertOperatorMapping("#x != 1", ComparisonOperator.NOT_EQUALS);
            assertOperatorMapping("#x > 1", ComparisonOperator.GREATER_THAN);
            assertOperatorMapping("#x < 1", ComparisonOperator.LESS_THAN);
            assertOperatorMapping("#x >= 1", ComparisonOperator.GREATER_THAN_OR_EQUAL);
            assertOperatorMapping("#x <= 1", ComparisonOperator.LESS_THAN_OR_EQUAL);
        }

        @Test
        @DisplayName("所有方法调用操作符映射正确")
        void should_map_all_method_operators() {
            assertOperatorMapping("#x.contains('a')", ComparisonOperator.CONTAINS);
            assertOperatorMapping("#x.startsWith('a')", ComparisonOperator.STARTS_WITH);
            assertOperatorMapping("#x.endsWith('a')", ComparisonOperator.ENDS_WITH);
            assertOperatorMapping("#x.isEmpty()", ComparisonOperator.IS_EMPTY);
        }

        private void assertOperatorMapping(String spel, ComparisonOperator expected) {
            ConditionItem item = factory.parseLegacySpelToItem(spel);
            assertNotNull(item, "Should parse: " + spel);
            assertEquals(expected, item.getOperator(),
                    "SpEL '" + spel + "' should map to " + expected);
        }
    }

    @Nested
    @DisplayName("左操作数格式")
    class LeftOperandTests {

        @Test
        @DisplayName("变量名映射为 inputs.{variable} 格式")
        void should_prefix_with_inputs() {
            ConditionItem item = factory.parseLegacySpelToItem("#myVariable > 0");

            assertNotNull(item);
            assertEquals("inputs.myVariable", item.getLeftOperand());
        }

        @Test
        @DisplayName("下划线变量名正确映射")
        void should_handle_underscore_variable() {
            ConditionItem item = factory.parseLegacySpelToItem("#my_var == 'test'");

            assertNotNull(item);
            assertEquals("inputs.my_var", item.getLeftOperand());
        }
    }
}
