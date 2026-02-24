package com.zj.aiagent.infrastructure.swarm.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 蜂群内置工具注册表：定义 6 个工具的 JSON Schema
 */
@Component
@RequiredArgsConstructor
public class SwarmToolRegistry {

    private final ObjectMapper objectMapper;

    public List<OpenAiApi.FunctionTool> getAllTools() {
        return List.of(
                buildTool("create", "创建子Agent", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "role", Map.of("type", "string", "description", "子Agent的角色名称，如 coder/researcher/reviewer")
                        ),
                        "required", List.of("role")
                )),
                buildTool("send", "向指定Agent发送消息", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "agent_id", Map.of("type", "integer", "description", "目标Agent的ID"),
                                "message", Map.of("type", "string", "description", "消息内容")
                        ),
                        "required", List.of("agent_id", "message")
                )),
                buildTool("self", "返回自身信息（id/role/workspace）", Map.of(
                        "type", "object",
                        "properties", Map.of()
                )),
                buildTool("list_agents", "列出当前workspace所有Agent", Map.of(
                        "type", "object",
                        "properties", Map.of()
                )),
                buildTool("send_group_message", "向群组发送消息", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "group_id", Map.of("type", "integer", "description", "群组ID"),
                                "message", Map.of("type", "string", "description", "消息内容")
                        ),
                        "required", List.of("group_id", "message")
                )),
                buildTool("list_groups", "列出可见群组", Map.of(
                        "type", "object",
                        "properties", Map.of()
                ))
        );
    }

    private OpenAiApi.FunctionTool buildTool(String name, String description, Map<String, Object> parameters) {
        try {
            String parametersJson = objectMapper.writeValueAsString(parameters);
            return new OpenAiApi.FunctionTool(
                    new OpenAiApi.FunctionTool.Function(description, name, parametersJson)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to build tool schema: " + name, e);
        }
    }
}
