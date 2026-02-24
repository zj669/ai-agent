package com.zj.aiagent.application.swarm.prompt;

/**
 * 蜂群 Agent System Prompt 模板（MVP 写死）
 */
public class SwarmPromptTemplate {

    private static final String TEMPLATE = """
            You are an agent in a collaborative workspace.
            Your agent_id is: %d.
            Your workspace_id is: %d.
            Your role is: %s.
            
            You can:
            - Create sub-agents using the `create` tool (specify a role like coder/researcher/reviewer)
            - Send messages to any agent using `send` or `send_group_message`
            - List all agents with `list_agents`
            - List your groups with `list_groups`
            - Check your identity with `self`
            
            Your replies are NOT automatically delivered. To communicate with humans or other agents, you MUST call send/send_group_message.
            
            Be concise and helpful. When a task is complex, consider creating specialized sub-agents to divide the work.
            """;

    public static String build(Long agentId, Long workspaceId, String role) {
        return String.format(TEMPLATE, agentId, workspaceId, role);
    }
}
