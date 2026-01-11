package com.zj.aiagent.domain.agent.entity;

import com.zj.aiagent.domain.agent.service.GraphValidator;
import com.zj.aiagent.domain.agent.valobj.AgentStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Agent Aggregate Root
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agent {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String icon;

    /**
     * Working Draft of the Graph
     */
    private String graphJson;

    private AgentStatus status;

    /**
     * Pointer to the currently active published version
     */
    private Long publishedVersionId;

    /**
     * Optimistic Locking Version
     */
    private Integer version;

    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // --- Business Behaviors ---

    /**
     * Security Check: Verify ownership
     */
    public boolean isOwnedBy(Long requestUserId) {
        return this.userId != null && this.userId.equals(requestUserId);
    }

    /**
     * Update configuration with Optimistic Locking check
     */
    public void updateConfig(String name, String description, String icon, String graphJson, Integer expectedVersion) {
        if (!Objects.equals(this.version, expectedVersion)) {
            throw new ConcurrentModificationException("当前配置已被修改，请刷新页面重新获取最新的配置");
        }

        this.name = name;
        this.description = description;
        this.icon = icon;
        this.graphJson = graphJson;
        // status remains unchanged (e.g. keeps DRAFT or PUBLISHED? usually editing
        // makes it DRAFT?)
        // Requirement usually says editing active agent puts it in DRAFT state
        // effectively for the graph_json part
        // But status is explicitly managed.
        // Let's assume editing doesn't auto-change status unless explicitly requested,
        // OR standard logic is: if Published, editing Draft doesn't change Status
        // 'PUBLISHED' which refers to the live version?
        // Wait, requirements say: "Status: DRAFT(0), PUBLISHED(1)".
        // If I publish, status is PUBLISHED. If I then edit, I am editing the
        // properties.
        // Usually `graph_json` IS the draft. `agent_version` is the published one.
        // So status 'PUBLISHED' might mean "There is a published version active".
        // Or it might mean "The current head is consistent with published".
        // Let's stick to simple logic: Status reflects the 'Agent' lifecycle.
        // If we want to mark it as having 'Unpublished Changes', that's often UI logic
        // comparing Draft vs Published.
        // Re-reading requirements: "回滚后默认为草稿".
        // So let's leave status changes to explicit transitions like publish/rollback.

        this.updateTime = LocalDateTime.now();
    }

    /**
     * Publish current draft
     */
    public AgentVersion publish(GraphValidator validator) {
        // 1. Validate
        validator.validate(this.graphJson);

        // 2. Update Status
        this.status = AgentStatus.PUBLISHED;
        this.updateTime = LocalDateTime.now();

        // 3. Create Version
        // Version number strategy: this logic might need repository to find max
        // version,
        // OR we just assume Service passes the next version number?
        // Domain entity usually doesn't know about DB state.
        // So we might return a 'proto' AgentVersion with null version number, to be
        // filled by Service/Repo?
        // Or we pass `nextVersion` as arg.
        // The Prompt said: "validator" is passed.
        // Let's assume the Service calculates nextVersion and we construct the entity.
        // BUT the task says "Implement behaviors: publish(validator)".
        // It's cleaner if we return a partial AgentVersion and let Service handle the
        // numbering,
        // OR we pass nextVersion to this method.
        // Given complexity, let's pass nextVersion.
        throw new UnsupportedOperationException("Use overloaded publish(validator, nextVersion)");
    }

    public AgentVersion publish(GraphValidator validator, Integer nextVersion) {
        validator.validate(this.graphJson);
        this.status = AgentStatus.PUBLISHED;
        this.updateTime = LocalDateTime.now();

        return AgentVersion.builder()
                .agentId(this.id)
                .version(nextVersion)
                .graphSnapshot(this.graphJson)
                .description("Published on " + LocalDateTime.now())
                .build();
    }

    /**
     * Rollback to a specific historical version
     */
    public void rollbackTo(AgentVersion targetVersion) {
        if (!this.id.equals(targetVersion.getAgentId())) {
            throw new IllegalArgumentException("Version does not belong to this agent");
        }

        this.graphJson = targetVersion.getGraphSnapshot();
        this.status = AgentStatus.DRAFT;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * Clone this agent properties to a new one
     */
    public Agent clone(String newName) {
        return Agent.builder()
                .userId(this.userId) // Default to same owner
                .name(newName)
                .description(this.description)
                .icon(this.icon)
                .graphJson(this.graphJson)
                .status(AgentStatus.DRAFT)
                .version(1) // Initial version
                .createTime(LocalDateTime.now())
                .build();
    }

    public void disable() {
        this.status = AgentStatus.DISABLED;
        this.updateTime = LocalDateTime.now();
    }
}
