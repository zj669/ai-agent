package com.zj.aiagent.domain.agent.entity;

import com.zj.aiagent.domain.agent.valobj.AgentStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ConcurrentModificationException;

/**
 * Agent Aggregate Root Unit Test
 * Focus: Optimistic locking, ownership, publish/rollback
 */
public class AgentTest {

    private Agent agent;

    @BeforeEach
    void setUp() {
        agent = Agent.builder()
                .id(1L)
                .userId(100L)
                .name("Test Agent")
                .description("Test Description")
                .graphJson("{}")
                .status(AgentStatus.DRAFT)
                .version(1)
                .build();
    }

    // --- Ownership Tests ---

    @Test
    @DisplayName("isOwnedBy returns true for correct user")
    void testIsOwnedByCorrectUser() {
        Assertions.assertTrue(agent.isOwnedBy(100L));
    }

    @Test
    @DisplayName("isOwnedBy returns false for wrong user")
    void testIsOwnedByWrongUser() {
        Assertions.assertFalse(agent.isOwnedBy(999L));
    }

    // --- Optimistic Locking Tests ---

    @Test
    @DisplayName("updateConfig succeeds with correct version")
    void testUpdateConfigSuccess() {
        String newGraph = "{\"nodes\":[]}";
        Assertions.assertDoesNotThrow(() -> agent.updateConfig("New Name", "New Desc", "icon.png", newGraph, 1));
        Assertions.assertEquals("New Name", agent.getName());
        Assertions.assertEquals(newGraph, agent.getGraphJson());
    }

    @Test
    @DisplayName("updateConfig fails with wrong version - Optimistic Lock")
    void testUpdateConfigOptimisticLockFailure() {
        ConcurrentModificationException ex = Assertions.assertThrows(
                ConcurrentModificationException.class,
                () -> agent.updateConfig("Name", "Desc", "icon.png", "{}", 999));
        Assertions.assertTrue(ex.getMessage().contains("已被修改"));
    }

    // --- Rollback Tests ---

    @Test
    @DisplayName("rollbackTo restores graphJson and sets DRAFT status")
    void testRollbackTo() {
        AgentVersion version = AgentVersion.builder()
                .agentId(1L)
                .version(1)
                .graphSnapshot("{\"restored\":true}")
                .build();

        agent.rollbackTo(version);

        Assertions.assertEquals("{\"restored\":true}", agent.getGraphJson());
        Assertions.assertEquals(AgentStatus.DRAFT, agent.getStatus());
    }

    @Test
    @DisplayName("rollbackTo fails if version belongs to different agent")
    void testRollbackToDifferentAgent() {
        AgentVersion wrongVersion = AgentVersion.builder()
                .agentId(999L) // Different agent
                .version(1)
                .graphSnapshot("{}")
                .build();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> agent.rollbackTo(wrongVersion));
    }

    // --- Clone Tests ---

    @Test
    @DisplayName("clone creates new agent with DRAFT status")
    void testClone() {
        Agent cloned = agent.clone("Cloned Agent");

        Assertions.assertNull(cloned.getId()); // New agent has no ID yet
        Assertions.assertEquals("Cloned Agent", cloned.getName());
        Assertions.assertEquals(agent.getGraphJson(), cloned.getGraphJson());
        Assertions.assertEquals(AgentStatus.DRAFT, cloned.getStatus());
        Assertions.assertEquals(1, cloned.getVersion());
    }
}
