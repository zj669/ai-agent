package com.zj.aiagent.infrastructure.workflow.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.entity.Edge;
import com.zj.aiagent.domain.workflow.valobj.*;
import com.zj.aiagent.infrastructure.workflow.graph.converter.NodeConfigConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * WorkflowGraphFactoryImpl 旧模型兼容转换单元测试
 * 测试 convertLegacyEdgesToBranches 和 parseLegacySpelToItem 方法
 */
class WorkflowGraphFactoryLegacyConversionTest {

    private WorkflowGraphFactoryImpl factory;

    @BeforeEach
    void setUp() {
        factory = new WorkflowGraphFactoryImpl(
                new ObjectMapper(),
                mock(NodeConfigConverter.class)
        );
    }

    // ========== convertLegacyEdgesToBranches 测试 ==========

    @Nested
    @DisplayName("convertLegacyEdgesToBranches")
    class ConvertLegacyEdgesToBranchesTest {

        @Test
        @DisplayName("should_convert_default_edge_to_default_branch")
        void should_convert_default_edge_to_default_branch() {
            Edge defaultEdge = Edge.builder()
                    .edgeId("e1")
                    .source("cond_1")
                    .target("node_2")
                    .edgeType(Edge.EdgeType.DEFAULT)
                    .build();

            List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(List.of(defaultEdge));

            assertThat(branches).hasSize(1);
            assertThat(branches.get(0).isDefault()).isTrue();
            assertThat(branches.get(0).getTargetNodeId()).isEqualTo("node_2");
            assertThat(branches.get(0).getPriority()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("should_convert_conditional_edge_with_parseable_spel_to_non_default_branch")
        void should_convert_conditional_edge_with_parseable_spel_to_non_default_branch() {
            Edge conditionalEdge = Edge.builder()
                    .edgeId("e1")
                    .source("cond_1")
                    .target("node_3")
                    .condition("#input > 100")
                    .edgeType(Edge.EdgeType.CONDITIONAL)
                    .build();

            Edge defaultEdge = Edge.builder()
                    .edgeId("e2")
                    .source("cond_1")
                    .target("node_4")
                    .edgeType(Edge.EdgeType.DEFAULT)
                    .build();

            List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(
                    List.of(conditionalEdge, defaultEdge));

            assertThat(branches).hasSize(2);

            // 第一个分支：非 default，有条件
            ConditionBranch condBranch = branches.get(0);
            assertThat(condBranch.isDefault()).isFalse();
            assertThat(condBranch.getTargetNodeId()).isEqualTo("node_3");
            assertThat(condBranch.getPriority()).isEqualTo(0);
            assertThat(condBranch.getConditionGroups()).hasSize(1);

            ConditionGroup group = condBranch.getConditionGroups().get(0);
            assertThat(group.getOperator()).isEqualTo(LogicalOperator.AND);
            assertThat(group.getConditions()).hasSize(1);

            ConditionItem item = group.getConditions().get(0);
            assertThat(item.getLeftOperand()).isEqualTo("inputs.input");
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.GREATER_THAN);
            assertThat(item.getRightOperand()).isEqualTo(100L);

            // 第二个分支：default
            assertThat(branches.get(1).isDefault()).isTrue();
            assertThat(branches.get(1).getTargetNodeId()).isEqualTo("node_4");
        }

        @Test
        @DisplayName("should_treat_unparseable_spel_as_default_branch")
        void should_treat_unparseable_spel_as_default_branch() {
            Edge complexEdge = Edge.builder()
                    .edgeId("e1")
                    .source("cond_1")
                    .target("node_3")
                    .condition("#a > 10 && #b < 20")
                    .edgeType(Edge.EdgeType.CONDITIONAL)
                    .build();

            List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(List.of(complexEdge));

            assertThat(branches).hasSize(1);
            assertThat(branches.get(0).isDefault()).isTrue();
            assertThat(branches.get(0).getTargetNodeId()).isEqualTo("node_3");
        }

        @Test
        @DisplayName("should_assign_incremental_priorities_to_conditional_branches")
        void should_assign_incremental_priorities_to_conditional_branches() {
            Edge edge1 = Edge.builder()
                    .edgeId("e1").source("c").target("n1")
                    .condition("#x == 1").edgeType(Edge.EdgeType.CONDITIONAL).build();
            Edge edge2 = Edge.builder()
                    .edgeId("e2").source("c").target("n2")
                    .condition("#x == 2").edgeType(Edge.EdgeType.CONDITIONAL).build();
            Edge defaultEdge = Edge.builder()
                    .edgeId("e3").source("c").target("n3")
                    .edgeType(Edge.EdgeType.DEFAULT).build();

            List<ConditionBranch> branches = factory.convertLegacyEdgesToBranches(
                    List.of(edge1, edge2, defaultEdge));

            assertThat(branches).hasSize(3);
            assertThat(branches.get(0).getPriority()).isEqualTo(0);
            assertThat(branches.get(1).getPriority()).isEqualTo(1);
            assertThat(branches.get(2).getPriority()).isEqualTo(Integer.MAX_VALUE);
        }
    }

    // ========== parseLegacySpelToItem 测试 ==========

    @Nested
    @DisplayName("parseLegacySpelToItem - 比较操作符")
    class ParseComparisonTest {

        @Test
        @DisplayName("should_parse_greater_than_with_integer")
        void should_parse_greater_than_with_integer() {
            ConditionItem item = factory.parseLegacySpelToItem("#input > 100");

            assertThat(item).isNotNull();
            assertThat(item.getLeftOperand()).isEqualTo("inputs.input");
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.GREATER_THAN);
            assertThat(item.getRightOperand()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should_parse_equals_with_string")
        void should_parse_equals_with_string() {
            ConditionItem item = factory.parseLegacySpelToItem("#name == 'hello'");

            assertThat(item).isNotNull();
            assertThat(item.getLeftOperand()).isEqualTo("inputs.name");
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.EQUALS);
            assertThat(item.getRightOperand()).isEqualTo("hello");
        }

        @Test
        @DisplayName("should_parse_not_equals")
        void should_parse_not_equals() {
            ConditionItem item = factory.parseLegacySpelToItem("#status != 'active'");

            assertThat(item).isNotNull();
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.NOT_EQUALS);
            assertThat(item.getRightOperand()).isEqualTo("active");
        }

        @Test
        @DisplayName("should_parse_less_than")
        void should_parse_less_than() {
            ConditionItem item = factory.parseLegacySpelToItem("#count < 50");

            assertThat(item).isNotNull();
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.LESS_THAN);
            assertThat(item.getRightOperand()).isEqualTo(50L);
        }

        @Test
        @DisplayName("should_parse_greater_than_or_equal")
        void should_parse_greater_than_or_equal() {
            ConditionItem item = factory.parseLegacySpelToItem("#score >= 90");

            assertThat(item).isNotNull();
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.GREATER_THAN_OR_EQUAL);
            assertThat(item.getRightOperand()).isEqualTo(90L);
        }

        @Test
        @DisplayName("should_parse_less_than_or_equal")
        void should_parse_less_than_or_equal() {
            ConditionItem item = factory.parseLegacySpelToItem("#age <= 18");

            assertThat(item).isNotNull();
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.LESS_THAN_OR_EQUAL);
            assertThat(item.getRightOperand()).isEqualTo(18L);
        }

        @Test
        @DisplayName("should_parse_equals_with_double")
        void should_parse_equals_with_double() {
            ConditionItem item = factory.parseLegacySpelToItem("#price == 3.14");

            assertThat(item).isNotNull();
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.EQUALS);
            assertThat(item.getRightOperand()).isEqualTo(3.14);
        }

        @Test
        @DisplayName("should_parse_equals_with_boolean_true")
        void should_parse_equals_with_boolean_true() {
            ConditionItem item = factory.parseLegacySpelToItem("#flag == true");

            assertThat(item).isNotNull();
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.EQUALS);
            assertThat(item.getRightOperand()).isEqualTo(Boolean.TRUE);
        }
    }

    @Nested
    @DisplayName("parseLegacySpelToItem - 方法调用")
    class ParseMethodCallTest {

        @Test
        @DisplayName("should_parse_contains")
        void should_parse_contains() {
            ConditionItem item = factory.parseLegacySpelToItem("#text.contains('hello')");

            assertThat(item).isNotNull();
            assertThat(item.getLeftOperand()).isEqualTo("inputs.text");
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.CONTAINS);
            assertThat(item.getRightOperand()).isEqualTo("hello");
        }

        @Test
        @DisplayName("should_parse_startsWith")
        void should_parse_startsWith() {
            ConditionItem item = factory.parseLegacySpelToItem("#name.startsWith('Dr')");

            assertThat(item).isNotNull();
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.STARTS_WITH);
            assertThat(item.getRightOperand()).isEqualTo("Dr");
        }

        @Test
        @DisplayName("should_parse_endsWith")
        void should_parse_endsWith() {
            ConditionItem item = factory.parseLegacySpelToItem("#file.endsWith('.pdf')");

            assertThat(item).isNotNull();
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.ENDS_WITH);
            assertThat(item.getRightOperand()).isEqualTo(".pdf");
        }

        @Test
        @DisplayName("should_parse_isEmpty")
        void should_parse_isEmpty() {
            ConditionItem item = factory.parseLegacySpelToItem("#value.isEmpty()");

            assertThat(item).isNotNull();
            assertThat(item.getLeftOperand()).isEqualTo("inputs.value");
            assertThat(item.getOperator()).isEqualTo(ComparisonOperator.IS_EMPTY);
            assertThat(item.getRightOperand()).isNull();
        }
    }

    @Nested
    @DisplayName("parseLegacySpelToItem - 无法解析的表达式")
    class ParseUnparseableTest {

        @Test
        @DisplayName("should_return_null_for_null_input")
        void should_return_null_for_null_input() {
            assertThat(factory.parseLegacySpelToItem(null)).isNull();
        }

        @Test
        @DisplayName("should_return_null_for_blank_input")
        void should_return_null_for_blank_input() {
            assertThat(factory.parseLegacySpelToItem("  ")).isNull();
        }

        @Test
        @DisplayName("should_return_null_for_complex_and_expression")
        void should_return_null_for_complex_and_expression() {
            assertThat(factory.parseLegacySpelToItem("#a > 10 && #b < 20")).isNull();
        }

        @Test
        @DisplayName("should_return_null_for_complex_or_expression")
        void should_return_null_for_complex_or_expression() {
            assertThat(factory.parseLegacySpelToItem("#a == 1 || #b == 2")).isNull();
        }

        @Test
        @DisplayName("should_return_null_for_nested_method_call")
        void should_return_null_for_nested_method_call() {
            assertThat(factory.parseLegacySpelToItem("#a.toString().length()")).isNull();
        }

        @Test
        @DisplayName("should_return_null_for_ternary_expression")
        void should_return_null_for_ternary_expression() {
            assertThat(factory.parseLegacySpelToItem("#a > 0 ? 'yes' : 'no'")).isNull();
        }
    }
}
