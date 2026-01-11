package com.zj.aiagent.interfaces.agent.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Data;

public class AgentDTO {

    // Validation Groups
    public interface Create {
    }

    public interface Update {
    }

    public interface Publish {
    }

    @Data
    public static class AgentSaveReq {
        @Null(groups = Create.class, message = "ID must be null for create")
        @NotNull(groups = Update.class, message = "ID cannot be null for update")
        private Long id;

        @NotEmpty(groups = { Create.class, Update.class }, message = "Name cannot be empty")
        private String name;

        private String description;
        private String icon;

        // Graph JSON optional on create, usually required on update
        private String graphJson;

        @NotNull(groups = Update.class, message = "Version required for optimistic locking")
        private Integer version;
    }

    @Data
    public static class PublishReq {
        @NotNull(message = "Agent ID required")
        private Long id;
    }

    @Data
    public static class RollbackReq {
        @NotNull(message = "Agent ID required")
        private Long id;
        @NotNull(message = "Target version required")
        private Integer targetVersion;
    }

    @Data
    public static class DebugReq {
        @NotNull
        private Long agentId;
        private String inputMessage;
        private boolean debugMode = true; // Default to debug draft
    }
}
