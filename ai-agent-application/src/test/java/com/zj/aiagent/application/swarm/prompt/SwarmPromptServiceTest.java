package com.zj.aiagent.application.swarm.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.swarm.SwarmContextAnalyzer;
import com.zj.aiagent.application.swarm.tool.SwarmToolFilter;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.entity.SwarmWorkspace;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmWorkspaceRepository;
import com.zj.aiagent.domain.swarm.valobj.SwarmRole;
import com.zj.aiagent.infrastructure.mcp.adapter.McpToolCallbackAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SwarmPromptService 动态提示词服务测试")
class SwarmPromptServiceTest {

    private SwarmToolFilter toolFilter;
    private SwarmContextAnalyzer contextAnalyzer;
    private SwarmAgentRepository agentRepository;
    private SwarmWorkspaceRepository workspaceRepository;
    private McpToolCallbackAdapter mcpToolCallbackAdapter;
    private SwarmPromptService promptService;

    @BeforeEach
    void setUp() {
        toolFilter = new SwarmToolFilter();
        contextAnalyzer = mock(SwarmContextAnalyzer.class);
        agentRepository = mock(SwarmAgentRepository.class);
        workspaceRepository = mock(SwarmWorkspaceRepository.class);
        mcpToolCallbackAdapter = mock(McpToolCallbackAdapter.class);

        // Mock agentRepository to return empty list for context building
        when(agentRepository.findByWorkspaceId(anyLong())).thenReturn(List.of());
        // Mock contextAnalyzer to return null for merge
        when(contextAnalyzer.mergeWorkerContexts(java.util.List.of())).thenReturn(null);
        // Mock workspaceRepository to return a workspace with userId
        when(workspaceRepository.findById(anyLong())).thenReturn(Optional.empty());
        // Mock mcpToolCallbackAdapter to return embed mode (no MCP tools)
        when(mcpToolCallbackAdapter.shouldEmbedInPromptByUserId(any())).thenReturn(true);
        when(mcpToolCallbackAdapter.buildEmbeddableToolSectionByUserId(any())).thenReturn("【MCP 可用工具】\n（当前无可用 MCP 工具）");

        promptService = new SwarmPromptService(
            toolFilter, contextAnalyzer, agentRepository,
            workspaceRepository, mcpToolCallbackAdapter
        );
    }

    private SwarmAgent makeAgent(Long id, Long workspaceId, Long parentId, String description) {
        return SwarmAgent.builder()
            .id(id)
            .workspaceId(workspaceId)
            .parentId(parentId)
            .description(description)
            .build();
    }

    @Nested
    @DisplayName("getPrompt — 基础变量替换")
    class BasicVariableReplacement {

        @Test
        @DisplayName("提示词应包含 agentId / workspaceId / role / description")
        void shouldContainAllBaseVariables() {
            SwarmAgent agent = makeAgent(5L, 10L, 2L, "测试协调者");
            String prompt = promptService.getPrompt(agent, SwarmRole.COORDINATOR);

            assertThat(prompt).contains("agent_id: 5");
            assertThat(prompt).contains("workspace_id: 10");
            assertThat(prompt).contains("描述: 测试协调者");
        }

        @Test
        @DisplayName("BASE Section 不含 parentAgentId（已移除该字段）")
        void shouldNotContainParentAgentId() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String prompt = promptService.getPrompt(agent, SwarmRole.COORDINATOR);

            // parentAgentId 字段已从 BASE Section 移除
            assertThat(prompt).doesNotContain("父 Agent ID:");
        }
    }

    @Nested
    @DisplayName("getPrompt — 角色差异")
    class RoleBasedDifference {

        @Test
        @DisplayName("COORDINATOR 提示词包含 Phase 工作流")
        void shouldContainPhaseWorkflowForCoordinator() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String prompt = promptService.getPrompt(agent, SwarmRole.COORDINATOR);

            assertThat(prompt).contains("RESEARCH");
            assertThat(prompt).contains("SYNTHESIS");
            assertThat(prompt).contains("IMPLEMENTATION");
            assertThat(prompt).contains("VERIFICATION");
            assertThat(prompt).contains("你是 Coordinator（协调者）");
        }

        @Test
        @DisplayName("WORKER 提示词包含执行规则")
        void shouldContainExecutionRulesForWorker() {
            SwarmAgent agent = makeAgent(5L, 10L, 2L, "");
            String prompt = promptService.getPrompt(agent, SwarmRole.WORKER);

            assertThat(prompt).contains("你是 Worker（执行者）");
            assertThat(prompt).contains("submit_result");
            assertThat(prompt).doesNotContain("你是 Coordinator");
        }

        @Test
        @DisplayName("所有提示词都包含通信协议")
        void shouldContainCommunicationProtocol() {
            for (SwarmRole role : SwarmRole.values()) {
                SwarmAgent agent = SwarmAgent.builder()
                    .id(1L).workspaceId(1L).build();
                String prompt = promptService.getPrompt(agent, role);

                assertThat(prompt).contains("通信协议")
                    .withFailMessage("提示词应包含通信协议 (role=%s)", role);
            }
        }
    }

    @Nested
    @DisplayName("resolveVariables — 变量替换")
    class ResolveVariables {

        @Test
        @DisplayName("支持 {key} 形式占位符替换")
        void shouldReplacePlaceholderVariables() {
            String template = "agent_id: {agentId}, workspace: {workspaceId}";
            Map<String, String> ctx = Map.of("agentId", "100", "workspaceId", "5");

            String result = promptService.resolveVariables(template, ctx);

            assertThat(result).isEqualTo("agent_id: 100, workspace: 5");
        }

        @Test
        @DisplayName("未知占位符保持不变")
        void shouldKeepUnknownPlaceholders() {
            String template = "agent: {unknownKey}";
            Map<String, String> ctx = Map.of("otherKey", "value");

            String result = promptService.resolveVariables(template, ctx);

            assertThat(result).isEqualTo("agent: {unknownKey}");
        }

        @Test
        @DisplayName("null 模板返回 null")
        void shouldReturnNullForNullTemplate() {
            assertThat(promptService.resolveVariables(null, Map.of())).isNull();
        }

        @Test
        @DisplayName("null 值替换为空字符串")
        void shouldReplaceNullValueWithEmpty() {
            String template = "id: {id}";
            Map<String, String> ctx = new java.util.HashMap<>();
            ctx.put("id", null);

            String result = promptService.resolveVariables(template, ctx);

            assertThat(result).isEqualTo("id: ");
        }
    }

    @Nested
    @DisplayName("getWorkerPrompt — Worker 专用")
    class GetWorkerPrompt {

        @Test
        @DisplayName("getWorkerPrompt 包含指定 Phase")
        void shouldContainSpecifiedPhase() {
            SwarmAgent agent = makeAgent(5L, 10L, 2L, "");
            String prompt = promptService.getWorkerPrompt(agent, "RESEARCH");

            assertThat(prompt).contains("RESEARCH");
        }

        @Test
        @DisplayName("getWorkerPrompt 默认 Phase 为 RESEARCH（运行时动态流转）")
        void shouldDefaultToResearchPhase() {
            SwarmAgent agent = makeAgent(5L, 10L, 2L, "");
            String prompt = promptService.getWorkerPrompt(agent, null);

            assertThat(prompt).contains("RESEARCH");
        }
    }

    @Nested
    @DisplayName("customPrompt — 自定义附加提示")
    class CustomPrompt {

        @Test
        @DisplayName("null customPrompt 不输出额外 section")
        void shouldHandleNullCustomPrompt() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String prompt = promptService.getPrompt(agent, SwarmRole.COORDINATOR, null);

            assertThat(prompt).doesNotContain("自定义附加提示");
        }

        @Test
        @DisplayName("空 customPrompt 不输出额外 section")
        void shouldHandleBlankCustomPrompt() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String prompt = promptService.getPrompt(agent, SwarmRole.COORDINATOR, "   ");

            assertThat(prompt).doesNotContain("自定义附加提示");
        }

        @Test
        @DisplayName("有效 customPrompt 追加到末尾")
        void shouldAppendCustomPrompt() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String custom = "额外指令：使用 Python 完成";
            String prompt = promptService.getPrompt(agent, SwarmRole.COORDINATOR, custom);

            assertThat(prompt).contains("额外指令：使用 Python 完成");
            assertThat(prompt).endsWith(custom);
        }
    }

    @Nested
    @DisplayName("getCoordinatorPrompt — 快捷方法")
    class ShortcutMethods {

        @Test
        @DisplayName("getCoordinatorPrompt 等价于 getPrompt(role=COORDINATOR)")
        void shouldMatchGetPrompt() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "coordinator_desc");
            String shortcut = promptService.getCoordinatorPrompt(agent);
            String explicit = promptService.getPrompt(agent, SwarmRole.COORDINATOR);

            assertThat(shortcut).isEqualTo(explicit);
        }
    }
}
