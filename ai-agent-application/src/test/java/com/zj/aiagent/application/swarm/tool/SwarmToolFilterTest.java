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
        @DisplayName("COORDINATOR 应返回 6 个调度工具（含 executeWorkflow，不含 submit_result）")
        void shouldReturnCoordinatorTools() {
            Set<String> tools = toolFilter.getAllowedToolNames(SwarmRole.COORDINATOR);
            assertThat(tools).hasSize(6);
            assertThat(tools).containsExactlyInAnyOrder(
                "create_worker", "delegate_task", "send", "self", "listAgents", "executeWorkflow"
            );
            assertThat(tools).doesNotContain("submit_result");
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
        @DisplayName("ASSISTANT 应返回空集合")
        void shouldReturnEmptyForAssistant() {
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
        @DisplayName("COORDINATOR 允许 executeWorkflow")
        void shouldAllowExecuteWorkflowForCoordinator() {
            assertThat(toolFilter.isAllowed(SwarmRole.COORDINATOR, "executeWorkflow")).isTrue();
        }

        @Test
        @DisplayName("未知工具返回 false")
        void shouldReturnFalseForUnknownTool() {
            assertThat(toolFilter.isAllowed(SwarmRole.COORDINATOR, "unknown_tool")).isFalse();
        }

        @Test
        @DisplayName("mcp__ 前缀工具在所有角色下均返回 true（MCP 工具默认放行）")
        void shouldAllowMcpToolsForAllRoles() {
            assertThat(toolFilter.isAllowed(SwarmRole.COORDINATOR, "mcp__1__search")).isTrue();
            assertThat(toolFilter.isAllowed(SwarmRole.WORKER, "mcp__2__write")).isTrue();
            assertThat(toolFilter.isAllowed(SwarmRole.ASSISTANT, "mcp__3__any")).isTrue();
            assertThat(toolFilter.isAllowed(null, "mcp__1__tool")).isTrue();
        }

        @Test
        @DisplayName("null 工具名返回 false（不在白名单且不满足 mcp__ 前缀）")
        void shouldReturnFalseForNullToolName() {
            assertThat(toolFilter.isAllowed(SwarmRole.COORDINATOR, null)).isFalse();
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
        @DisplayName("ASSISTANT 的工具描述为空提示")
        void shouldBuildAssistantToolSection() {
            String section = toolFilter.buildToolSection(SwarmRole.ASSISTANT);
            assertThat(section).contains("无工具可用");
        }

        @Test
        @DisplayName("COORDINATOR 的工具描述包含 executeWorkflow 且不含 submit_result")
        void shouldBuildCoordinatorToolSection() {
            String section = toolFilter.buildToolSection(SwarmRole.COORDINATOR);
            assertThat(section).contains("executeWorkflow");
            assertThat(section).doesNotContain("submit_result");
        }
    }
}
