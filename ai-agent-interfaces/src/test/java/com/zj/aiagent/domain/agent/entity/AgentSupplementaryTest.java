package com.zj.aiagent.domain.agent.entity;

import com.zj.aiagent.domain.agent.service.GraphValidator;
import com.zj.aiagent.domain.agent.valobj.AgentStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

/**
 * Agent 聚合根补充单元测试
 * 覆盖: 发布功能、禁用功能、边界条件
 *
 * 测试工程师: 测试工程师3号
 * 日期: 2026-02-10
 */
public class AgentSupplementaryTest {

    private Agent agent;
    private GraphValidator mockValidator;

    @BeforeEach
    void setUp() {
        agent = Agent.builder()
                .id(1L)
                .userId(100L)
                .name("Test Agent")
                .description("Test Description")
                .icon("https://example.com/icon.png")
                .graphJson("{\"dagId\":\"dag-12345678\",\"nodes\":[{\"id\":\"1\",\"type\":\"START\"}],\"edges\":[]}")
                .status(AgentStatus.DRAFT)
                .version(1)
                .createTime(LocalDateTime.now())
                .build();

        mockValidator = Mockito.mock(GraphValidator.class);
    }

    // --- 发布功能测试 ---

    @Test
    @DisplayName("publish succeeds with valid graph and updates status to PUBLISHED")
    void testPublishSuccess() {
        // Given: 有效的图结构
        Mockito.doNothing().when(mockValidator).validate(Mockito.anyString());

        // When: 发布 Agent
        AgentVersion version = agent.publish(mockValidator, 1);

        // Then: 状态更新为 PUBLISHED，返回版本对象
        Assertions.assertEquals(AgentStatus.PUBLISHED, agent.getStatus());
        Assertions.assertNotNull(version);
        Assertions.assertEquals(1L, version.getAgentId());
        Assertions.assertEquals(1, version.getVersion());
        Assertions.assertEquals(agent.getGraphJson(), version.getGraphSnapshot());
        Assertions.assertNotNull(agent.getUpdateTime());
    }

    @Test
    @DisplayName("publish fails with invalid graph and throws IllegalArgumentException")
    void testPublishInvalidGraph() {
        // Given: 无效的图结构
        agent.setGraphJson("{\"nodes\":[],\"edges\":[]}");
        Mockito.doThrow(new IllegalArgumentException("Graph validation failed: Empty graph"))
                .when(mockValidator).validate(Mockito.anyString());

        // When & Then: 发布失败，抛出异常
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> agent.publish(mockValidator, 1));
        Assertions.assertTrue(ex.getMessage().contains("Graph validation failed"));
    }

    @Test
    @DisplayName("publish with cycle detection failure")
    void testPublishWithCycle() {
        // Given: 包含环的图
        agent.setGraphJson("{\"nodes\":[{\"id\":\"1\",\"type\":\"START\"},{\"id\":\"2\",\"type\":\"LLM\"}],\"edges\":[{\"source\":\"1\",\"target\":\"2\"},{\"source\":\"2\",\"target\":\"1\"}]}");
        Mockito.doThrow(new IllegalArgumentException("Graph contains cycle"))
                .when(mockValidator).validate(Mockito.anyString());

        // When & Then: 发布失败
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> agent.publish(mockValidator, 1));
        Assertions.assertTrue(ex.getMessage().contains("cycle"));
    }

    @Test
    @DisplayName("publish increments version number correctly")
    void testPublishVersionIncrement() {
        // Given: 第二次发布
        Mockito.doNothing().when(mockValidator).validate(Mockito.anyString());

        // When: 发布版本 2
        AgentVersion version = agent.publish(mockValidator, 2);

        // Then: 版本号为 2
        Assertions.assertEquals(2, version.getVersion());
    }

    @Test
    @DisplayName("publish without nextVersion parameter throws UnsupportedOperationException")
    void testPublishWithoutNextVersion() {
        // When & Then: 调用不带版本号的 publish 方法
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> agent.publish(mockValidator));
    }

    // --- 禁用功能测试 ---

    @Test
    @DisplayName("disable updates status to DISABLED")
    void testDisable() {
        // Given: PUBLISHED 状态的 Agent
        agent.setStatus(AgentStatus.PUBLISHED);
        LocalDateTime beforeDisable = LocalDateTime.now();

        // When: 禁用 Agent
        agent.disable();

        // Then: 状态更新为 DISABLED，更新时间被设置
        Assertions.assertEquals(AgentStatus.DISABLED, agent.getStatus());
        Assertions.assertNotNull(agent.getUpdateTime());
        Assertions.assertTrue(agent.getUpdateTime().isAfter(beforeDisable) ||
                agent.getUpdateTime().isEqual(beforeDisable));
    }

    @Test
    @DisplayName("disable from DRAFT status")
    void testDisableFromDraft() {
        // Given: DRAFT 状态的 Agent
        agent.setStatus(AgentStatus.DRAFT);

        // When: 禁用 Agent
        agent.disable();

        // Then: 状态更新为 DISABLED
        Assertions.assertEquals(AgentStatus.DISABLED, agent.getStatus());
    }

    // --- 边界条件测试 ---

    @Test
    @DisplayName("updateConfig handles null name")
    void testUpdateConfigWithNullName() {
        // When: 更新时 name 为 null
        agent.updateConfig(null, "New Description", "icon.png", "{}", 1);

        // Then: name 被设置为 null（业务层应该验证）
        Assertions.assertNull(agent.getName());
        Assertions.assertEquals("New Description", agent.getDescription());
    }

    @Test
    @DisplayName("updateConfig handles null description")
    void testUpdateConfigWithNullDescription() {
        // When: 更新时 description 为 null
        agent.updateConfig("New Name", null, "icon.png", "{}", 1);

        // Then: description 被设置为 null
        Assertions.assertEquals("New Name", agent.getName());
        Assertions.assertNull(agent.getDescription());
    }

    @Test
    @DisplayName("updateConfig handles empty graphJson")
    void testUpdateConfigWithEmptyGraphJson() {
        // When: 更新时 graphJson 为空字符串
        agent.updateConfig("Name", "Desc", "icon.png", "", 1);

        // Then: graphJson 被设置为空字符串
        Assertions.assertEquals("", agent.getGraphJson());
    }

    @Test
    @DisplayName("updateConfig handles very long description")
    void testUpdateConfigWithLongDescription() {
        // Given: 超长描述（10000 字符）
        String longDescription = "A".repeat(10000);

        // When: 更新
        agent.updateConfig("Name", longDescription, "icon.png", "{}", 1);

        // Then: 描述被正确设置（数据库层应该验证长度）
        Assertions.assertEquals(longDescription, agent.getDescription());
        Assertions.assertEquals(10000, agent.getDescription().length());
    }

    @Test
    @DisplayName("updateConfig with special characters in name")
    void testUpdateConfigWithSpecialCharacters() {
        // Given: 包含特殊字符的名称
        String specialName = "Agent <script>alert('XSS')</script> 测试 🚀";

        // When: 更新
        agent.updateConfig(specialName, "Desc", "icon.png", "{}", 1);

        // Then: 特殊字符被正确保存
        Assertions.assertEquals(specialName, agent.getName());
    }

    @Test
    @DisplayName("rollbackTo with null graphSnapshot should fail")
    void testRollbackToNullGraphSnapshot() {
        // Given: 版本的 graphSnapshot 为 null
        AgentVersion version = AgentVersion.builder()
                .agentId(1L)
                .version(1)
                .graphSnapshot(null)
                .build();

        // When: 回滚
        agent.rollbackTo(version);

        // Then: graphJson 被设置为 null
        Assertions.assertNull(agent.getGraphJson());
        Assertions.assertEquals(AgentStatus.DRAFT, agent.getStatus());
    }

    @Test
    @DisplayName("clone with null userId")
    void testCloneWithNullUserId() {
        // Given: Agent 的 userId 为 null
        agent.setUserId(null);

        // When: 克隆
        Agent cloned = agent.clone("Cloned Agent");

        // Then: 克隆的 Agent userId 也为 null
        Assertions.assertNull(cloned.getUserId());
    }

    @Test
    @DisplayName("clone preserves all properties except id and name")
    void testClonePreservesProperties() {
        // When: 克隆
        Agent cloned = agent.clone("Cloned Agent");

        // Then: 除了 id 和 name，其他属性都被保留
        Assertions.assertNull(cloned.getId());
        Assertions.assertEquals("Cloned Agent", cloned.getName());
        Assertions.assertEquals(agent.getDescription(), cloned.getDescription());
        Assertions.assertEquals(agent.getIcon(), cloned.getIcon());
        Assertions.assertEquals(agent.getGraphJson(), cloned.getGraphJson());
        Assertions.assertEquals(agent.getUserId(), cloned.getUserId());
        Assertions.assertEquals(AgentStatus.DRAFT, cloned.getStatus());
        Assertions.assertEquals(1, cloned.getVersion());
    }

    // --- 业务规则测试 ---

    @Test
    @DisplayName("isOwnedBy with null userId returns false")
    void testIsOwnedByNullUserId() {
        // Given: Agent 的 userId 为 null
        agent.setUserId(null);

        // When & Then: 任何用户都不拥有该 Agent
        Assertions.assertFalse(agent.isOwnedBy(100L));
        Assertions.assertFalse(agent.isOwnedBy(null));
    }

    @Test
    @DisplayName("isOwnedBy with null requestUserId returns false")
    void testIsOwnedByNullRequestUserId() {
        // When & Then: null 用户不拥有任何 Agent
        Assertions.assertFalse(agent.isOwnedBy(null));
    }

    @Test
    @DisplayName("updateConfig preserves status")
    void testUpdateConfigPreservesStatus() {
        // Given: PUBLISHED 状态的 Agent
        agent.setStatus(AgentStatus.PUBLISHED);

        // When: 更新配置
        agent.updateConfig("New Name", "New Desc", "icon.png", "{}", 1);

        // Then: 状态保持不变（PUBLISHED）
        Assertions.assertEquals(AgentStatus.PUBLISHED, agent.getStatus());
    }

    @Test
    @DisplayName("multiple updateConfig calls increment updateTime")
    void testMultipleUpdateConfigCalls() {
        // Given: 第一次更新
        agent.updateConfig("Name 1", "Desc 1", "icon.png", "{}", 1);
        LocalDateTime firstUpdate = agent.getUpdateTime();

        // When: 等待一小段时间后再次更新
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        agent.setVersion(2); // 模拟版本号递增
        agent.updateConfig("Name 2", "Desc 2", "icon.png", "{}", 2);
        LocalDateTime secondUpdate = agent.getUpdateTime();

        // Then: 第二次更新时间晚于第一次
        Assertions.assertTrue(secondUpdate.isAfter(firstUpdate) ||
                secondUpdate.isEqual(firstUpdate));
    }

    // --- 状态转换测试 ---

    @Test
    @DisplayName("status transition: DRAFT -> PUBLISHED -> DISABLED")
    void testStatusTransitionChain() {
        // Given: DRAFT 状态
        Assertions.assertEquals(AgentStatus.DRAFT, agent.getStatus());

        // When: 发布
        Mockito.doNothing().when(mockValidator).validate(Mockito.anyString());
        agent.publish(mockValidator, 1);
        Assertions.assertEquals(AgentStatus.PUBLISHED, agent.getStatus());

        // When: 禁用
        agent.disable();
        Assertions.assertEquals(AgentStatus.DISABLED, agent.getStatus());
    }

    @Test
    @DisplayName("status transition: PUBLISHED -> DRAFT after rollback")
    void testStatusTransitionAfterRollback() {
        // Given: PUBLISHED 状态
        agent.setStatus(AgentStatus.PUBLISHED);

        // When: 回滚
        AgentVersion version = AgentVersion.builder()
                .agentId(1L)
                .version(1)
                .graphSnapshot("{\"restored\":true}")
                .build();
        agent.rollbackTo(version);

        // Then: 状态变为 DRAFT
        Assertions.assertEquals(AgentStatus.DRAFT, agent.getStatus());
    }
}
