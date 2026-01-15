package com.zj.aiagent.application.agent.cmd;

import lombok.Data;

public class AgentCommand {
    @Data
    public static class CreateAgentCmd {
        private Long userId;
        private String name;
        private String description;
        private String icon;
    }

    @Data
    public static class UpdateAgentCmd {
        private Long id;
        private Long userId;
        private String name;
        private String description;
        private String icon;
        private String graphJson;
        private Integer version; // Optimistic Lock
    }

    @Data
    public static class PublishAgentCmd {
        private Long id;
        private Long userId;
    }

    @Data
    public static class RollbackAgentCmd {
        private Long id;
        private Long userId;
        private Integer targetVersion;
    }

    @Data
    public static class DeleteAgentCmd {
        private Long id;
        private Long userId;
    }

    @Data
    public static class DeleteVersionCmd {
        private Long agentId;
        private Long userId;
        private Integer version;
    }

    @Data
    public static class DebugAgentCmd {
        private Long agentId;
        private Long userId;
        private String inputMessage; // Initial input
        private boolean debugMode; // If true, run draft
    }
}
