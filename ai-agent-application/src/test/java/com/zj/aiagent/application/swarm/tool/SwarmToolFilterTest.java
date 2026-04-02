package com.zj.aiagent.application.swarm.tool;

import com.zj.aiagent.domain.swarm.valobj.SwarmRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SwarmToolFilter 工具白名单过滤测试")
class SwarmToolFilterTest {

    private SwarmToolFilter toolFilter;

    @BeforeEach
    void setUp() {
        toolFilter = new SwarmToolFilter();
    }

    @Nested
    @DisplayName("getAllowedToolNames — 角色白名单")
    class GetAllowedToolNames {

        @Test
        @DisplayName("ROOT 应返回全部 7 个工具")
        void shouldReturnAllToolsForRoot() {
            Set<String> tools = toolFilter.getAllowedToolNames(SwarmRole.ROOT);
            assertThat(tools).hasSize(7);
            assertThat(tools).containsExactlyInAnyOrder(
                "create_worker", "delegate_task", "submit_result",
                "send", "self", "listAgents", "executeWorkflow"
            );
        }

        @Test
        @DisplayName("COORDINATOR 应返回调度工具（不含 submit_result / executeWorkflow）")
        void shouldReturnCoordinatorTools() {
            Set<String> tools = toolFilter.getAllowedToolNames(SwarmRole.COORDINATOR);
            assertThat(tools).hasSize(5);
            assertThat(tools).containsExactlyInAnyOrder(
                "create_worker", "delegate_task", "send", "self", "listAgents"
            );
            assertThat(tools).doesNotContain("submit_result", "executeWorkflow");
        }

        @Test
        @DisplayName("WORKER 应返回执行工具（仅 submit_result / send / self）")
        void shouldReturnWorkerTools() {
            Set<String> tools = toolFilter.getAllowedToolNames(SwarmRole.WORKER);
            assertThat(tools).hasSize(3);
            assertThat(tools).containsExactlyInAnyOrder(
                "submit_result", "send", "self"
            );
            assertThat(tools).doesNotContain("create_worker", "delegate_task", "executeWorkflow", "listAgents");
        }

        @Test
        @DisplayName("HUMAN / ASSISTANT 应返回空集合")
        void shouldReturnEmptyForHumanAndAssistant() {
            assertThat(toolFilter.getAllowedToolNames(SwarmRole.HUMAN)).isEmpty();
            assertThat(toolFilter.getAllowedToolNames(SwarmRole.ASSISTANT)).isEmpty();
        }

        @Test
        @DisplayName("null 角色应返回空集合")
        void shouldReturnEmptyForNullRole() {
            assertThat(toolFilter.getAllowedToolNames(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("isAllowed — 单工具权限检查")
    class IsAllowed {

        @Test
        @DisplayName("COORDINATOR 不允许 submit_result")
        void shouldDisallowSubmitResultForCoordinator() {
            assertThat(toolFilter.isAllowed(SwarmRole.COORDINATOR, "submit_result")).isFalse();
        }

        @Test
        @DisplayName("WORKER 不允许 create_worker")
        void shouldDisallowCreateWorkerForWorker() {
            assertThat(toolFilter.isAllowed(SwarmRole.WORKER, "create_worker")).isFalse();
        }

        @Test
        @DisplayName("ROOT 允许 executeWorkflow")
        void shouldAllowExecuteWorkflowForRoot() {
            assertThat(toolFilter.isAllowed(SwarmRole.ROOT, "executeWorkflow")).isTrue();
        }

        @Test
        @DisplayName("未知工具返回 false")
        void shouldReturnFalseForUnknownTool() {
            assertThat(toolFilter.isAllowed(SwarmRole.ROOT, "unknown_tool")).isFalse();
        }
    }

    @Nested
    @DisplayName("buildToolSection — 工具描述列表生成")
    class BuildToolSection {

        @Test
        @DisplayName("WORKER 的工具描述只包含 3 个执行工具")
        void shouldBuildWorkerToolSection() {
            String section = toolFilter.buildToolSection(SwarmRole.WORKER);
            assertThat(section).contains("submit_result");
            assertThat(section).contains("send");
            assertThat(section).contains("self");
            assertThat(section).doesNotContain("create_worker");
        }

        @Test
        @DisplayName("HUMAN 的工具描述为空提示")
        void shouldBuildHumanToolSection() {
            String section = toolFilter.buildToolSection(SwarmRole.HUMAN);
            assertThat(section).contains("无工具可用");
        }

        @Test
        @DisplayName("ROOT 的工具描述包含 executeWorkflow")
        void shouldBuildRootToolSection() {
            String section = toolFilter.buildToolSection(SwarmRole.ROOT);
            assertThat(section).contains("executeWorkflow");
        }
    }
}
