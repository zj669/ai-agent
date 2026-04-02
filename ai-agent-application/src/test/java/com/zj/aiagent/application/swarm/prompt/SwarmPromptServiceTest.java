package com.zj.aiagent.application.swarm.prompt;

import com.zj.aiagent.application.swarm.tool.SwarmToolFilter;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.valobj.SwarmRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SwarmPromptService 动态提示词服务测试")
class SwarmPromptServiceTest {

    private SwarmToolFilter toolFilter;
    private SwarmPromptService promptService;

    @BeforeEach
    void setUp() {
        toolFilter = new SwarmToolFilter();
        promptService = new SwarmPromptService(toolFilter);
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
        @DisplayName("提示词应包含 agentId / workspaceId / role / description / parentAgentId")
        void shouldContainAllBaseVariables() {
            SwarmAgent agent = makeAgent(5L, 10L, 2L, "测试协调者");
            String prompt = promptService.getPrompt(agent, SwarmRole.COORDINATOR, 1L);

            assertThat(prompt).contains("agent_id: 5");
            assertThat(prompt).contains("workspace_id: 10");
            assertThat(prompt).contains("父 Agent ID: 2");
            assertThat(prompt).contains("描述: 测试协调者");
        }

        @Test
        @DisplayName("parentId 为 null 时显示 null")
        void shouldShowNullForMissingParentId() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String prompt = promptService.getPrompt(agent, SwarmRole.ROOT, 1L);

            assertThat(prompt).contains("父 Agent ID: null");
        }

        @Test
        @DisplayName("humanAgentId 应出现在禁止规则中")
        void shouldContainHumanAgentId() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String prompt = promptService.getPrompt(agent, SwarmRole.WORKER, 99L);

            // 提示词中应禁止对 role=human 的 agent 使用 send
            assertThat(prompt).contains("禁止对 role=human 的 agent 使用 send");
        }
    }

    @Nested
    @DisplayName("getPrompt — 角色差异")
    class RoleBasedDifference {

        @Test
        @DisplayName("COORDINATOR 提示词包含 Phase 工作流")
        void shouldContainPhaseWorkflowForCoordinator() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String prompt = promptService.getPrompt(agent, SwarmRole.COORDINATOR, 1L);

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
            String prompt = promptService.getPrompt(agent, SwarmRole.WORKER, 1L);

            assertThat(prompt).contains("你是 Worker（执行者）");
            assertThat(prompt).contains("submit_result");
            assertThat(prompt).doesNotContain("你是 Coordinator");
        }

        @Test
        @DisplayName("ROOT 提示词包含全部工具")
        void shouldContainAllToolsForRoot() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String prompt = promptService.getPrompt(agent, SwarmRole.ROOT, 1L);

            assertThat(prompt).contains("create_worker");
            assertThat(prompt).contains("executeWorkflow");
            assertThat(prompt).doesNotContain("你是 Coordinator");
        }

        @Test
        @DisplayName("所有提示词都包含通信协议")
        void shouldContainCommunicationProtocol() {
            for (SwarmRole role : SwarmRole.values()) {
                SwarmAgent agent = SwarmAgent.builder()
                    .id(1L).workspaceId(1L).build();
                String prompt = promptService.getPrompt(agent, role, 99L);

                assertThat(prompt).contains("通信协议")
                    .withFailMessage("提示词应包含通信协议 (role=%s)", role);
                assertThat(prompt).contains("send")
                    .withFailMessage("提示词应提及 send 工具 (role=%s)", role);
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
            String prompt = promptService.getWorkerPrompt(agent, 1L, "RESEARCH");

            assertThat(prompt).contains("RESEARCH");
        }

        @Test
        @DisplayName("getWorkerPrompt 默认 Phase 为 IMPLEMENTATION")
        void shouldDefaultToImplementationPhase() {
            SwarmAgent agent = makeAgent(5L, 10L, 2L, "");
            String prompt = promptService.getWorkerPrompt(agent, 1L, null);

            assertThat(prompt).contains("IMPLEMENTATION");
        }
    }

    @Nested
    @DisplayName("customPrompt — 自定义附加提示")
    class CustomPrompt {

        @Test
        @DisplayName("null customPrompt 不输出额外 section")
        void shouldHandleNullCustomPrompt() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String prompt = promptService.getPrompt(agent, SwarmRole.ROOT, 1L, null);

            assertThat(prompt).doesNotContain("自定义附加提示");
        }

        @Test
        @DisplayName("空 customPrompt 不输出额外 section")
        void shouldHandleBlankCustomPrompt() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String prompt = promptService.getPrompt(agent, SwarmRole.ROOT, 1L, "   ");

            assertThat(prompt).doesNotContain("自定义附加提示");
        }

        @Test
        @DisplayName("有效 customPrompt 追加到末尾")
        void shouldAppendCustomPrompt() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "");
            String custom = "额外指令：使用 Python 完成";
            String prompt = promptService.getPrompt(agent, SwarmRole.COORDINATOR, 1L, custom);

            assertThat(prompt).contains("额外指令：使用 Python 完成");
            assertThat(prompt).endsWith(custom);
        }
    }

    @Nested
    @DisplayName("getCoordinatorPrompt / getRootPrompt — 快捷方法")
    class ShortcutMethods {

        @Test
        @DisplayName("getCoordinatorPrompt 等价于 getPrompt(role=COORDINATOR)")
        void shouldMatchGetPrompt() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "coordinator_desc");
            String shortcut = promptService.getCoordinatorPrompt(agent, 1L);
            String explicit = promptService.getPrompt(agent, SwarmRole.COORDINATOR, 1L);

            assertThat(shortcut).isEqualTo(explicit);
        }

        @Test
        @DisplayName("getRootPrompt 等价于 getPrompt(role=ROOT)")
        void shouldMatchGetRootPrompt() {
            SwarmAgent agent = makeAgent(5L, 10L, null, "root_desc");
            String shortcut = promptService.getRootPrompt(agent, 1L);
            String explicit = promptService.getPrompt(agent, SwarmRole.ROOT, 1L);

            assertThat(shortcut).isEqualTo(explicit);
        }
    }
}
