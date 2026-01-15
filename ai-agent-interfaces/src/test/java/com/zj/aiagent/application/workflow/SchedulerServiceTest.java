package com.zj.aiagent.application.workflow;

import com.zj.aiagent.domain.agent.repository.AgentRepository;
import com.zj.aiagent.domain.chat.port.ConversationRepository;
import com.zj.aiagent.domain.memory.port.VectorStore;
import com.zj.aiagent.domain.workflow.entity.Execution;
import com.zj.aiagent.domain.workflow.entity.HumanReviewRecord;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.port.*;
import com.zj.aiagent.domain.workflow.service.WorkflowGraphFactory;
import com.zj.aiagent.domain.workflow.valobj.*;
import com.zj.aiagent.infrastructure.workflow.executor.NodeExecutorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Package Verified: com.zj.aiagent.application.workflow (Matches Application Root)
@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulerService HITL 测试")
class SchedulerServiceTest {

    // ========== Mocks for SchedulerService Dependencies ==========
    @Mock
    private NodeExecutorFactory executorFactory;
    @Mock
    private ExecutionRepository executionRepository;
    @Mock
    private CheckpointRepository checkpointRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private WorkflowGraphFactory workflowGraphFactory;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private StreamPublisherFactory streamPublisherFactory;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private HumanReviewRepository humanReviewRepository;
    @Mock
    private VectorStore vectorStore;
    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private SchedulerService schedulerService;

    // ========== Test Fixtures ==========
    @Mock
    private Execution execution;
    @Mock
    private WorkflowGraph workflowGraph;
    @Mock
    private Node node;
    @Mock
    private StreamPublisher streamPublisher;
    @Mock
    private RLock rLock;
    @Mock
    private RSet<String> rSet;
    @Mock
    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        // Common Lenient Stubs for Redisson
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        // Use doReturn to avoid generic type mismatch with RSet<String> vs RSet<Object>
        lenient().doReturn(rSet).when(redissonClient).getSet(anyString());
        lenient().doNothing().when(rLock).lock(anyLong(), any(TimeUnit.class));
        lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);
        lenient().doNothing().when(rLock).unlock();

        // Common Lenient Stubs for Stream
        lenient().when(streamPublisherFactory.create(any())).thenReturn(streamPublisher);
    }

    // ========== Test Cases ==========

    @Nested
    @DisplayName("resumeExecution 测试")
    class ResumeExecutionTests {

        @Test
        @DisplayName("成功恢复 BEFORE_EXECUTION 阶段暂停的工作流")
        void should_ResumeAndPublishEvent_When_PausedBeforeExecution() {
            // Given
            String executionId = "exec-resume-001";
            String nodeId = "node-llm-001";
            Long reviewerId = 123L;
            String comment = "LGTM";
            Map<String, Object> edits = Map.of("prompt", "Updated prompt");

            // Mock Node properties (Crucial for scheduleNode)
            lenient().when(node.getNodeId()).thenReturn(nodeId);
            lenient().when(node.getType()).thenReturn(NodeType.LLM);
            lenient().when(node.getName()).thenReturn("Test LLM Node");
            lenient().when(node.getInputs()).thenReturn(Collections.emptyMap());
            lenient().when(node.requiresHumanReview()).thenReturn(false);

            // Mock Execution behavior
            when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
            when(execution.getPausedPhase()).thenReturn(TriggerPhase.BEFORE_EXECUTION);
            when(execution.resume(eq(nodeId), anyMap())).thenReturn(Collections.singletonList(node));
            lenient().when(execution.getExecutionId()).thenReturn(executionId);
            lenient().when(execution.getContext()).thenReturn(executionContext);
            lenient().when(executionContext.resolveInputs(anyMap())).thenReturn(new java.util.HashMap<>());

            // Mock Executor Strategy
            com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy strategy = mock(
                    com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy.class);
            lenient().when(executorFactory.getStrategy(NodeType.LLM)).thenReturn(strategy);
            lenient().when(strategy.executeAsync(any(), any(), any())).thenReturn(java.util.concurrent.CompletableFuture
                    .completedFuture(NodeExecutionResult.success(Map.of("res", "done"))));

            // When
            schedulerService.resumeExecution(executionId, nodeId, edits, reviewerId, comment);

            // Then
            // 1. HumanReviewRecord saved
            verify(humanReviewRepository).save(any(HumanReviewRecord.class));

            // 2. Execution state updated via Domain method
            verify(execution).resume(eq(nodeId), eq(edits));

            // 3. Execution persisted
            verify(executionRepository).update(execution);

            // 4. SSE event published
            verify(streamPublisher).publishEvent(eq("workflow_resumed"), anyMap());

            // 5. Removed from pending set
            verify(rSet).remove(executionId);
        }

        @Test
        @DisplayName("Execution 不存在时应抛出异常")
        void should_ThrowException_When_ExecutionNotFound() {
            // Given
            String unknownId = "exec-unknown";
            when(executionRepository.findById(unknownId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> schedulerService.resumeExecution(unknownId, "node-1", null, 1L, ""));
        }

        @Test
        @DisplayName("Execution 已取消时应提前返回")
        void should_ReturnEarly_When_ExecutionCancelled() {
            // Given
            String executionId = "exec-cancelled-001";
            when(redisTemplate.hasKey("workflow:cancel:" + executionId)).thenReturn(true);

            // When
            schedulerService.resumeExecution(executionId, "node-1", null, 1L, "");

            // Then
            verify(executionRepository, never()).findById(anyString());
            verify(humanReviewRepository, never()).save(any());
        }
    }
}
