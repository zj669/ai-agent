package com.zj.aiagent.application.agent.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

/**
 * Agent 模块请求对象
 * 用于接收前端请求参数，包含校验注解
 */
public class AgentRequest {

    // Validation Groups
    public interface Create {
    }

    public interface Update {
    }

    /**
     * 创建/更新智能体请求
     */
    @Data
    public static class SaveAgentRequest {
        @Null(groups = Create.class, message = "ID must be null for create")
        @NotNull(groups = Update.class, message = "ID cannot be null for update")
        private Long id;

        @NotEmpty(groups = { Create.class, Update.class }, message = "Name cannot be empty")
        private String name;

        private String description;
        private String icon;
        private String graphJson;

        @NotNull(groups = Update.class, message = "Version required for optimistic locking")
        private Integer version;
    }

    /**
     * 发布智能体请求
     */
    @Data
    public static class PublishAgentRequest {
        @NotNull(message = "Agent ID required")
        private Long id;
    }

    /**
     * 回滚智能体请求
     */
    @Data
    public static class RollbackAgentRequest {
        @NotNull(message = "Agent ID required")
        private Long id;

        @NotNull(message = "Target version required")
        private Integer targetVersion;
    }

    /**
     * 调试智能体请求
     */
    @Data
    public static class DebugAgentRequest {
        @NotNull(message = "Agent ID required")
        private Long agentId;

        private String inputMessage;
        private boolean debugMode = true;
    }
}
