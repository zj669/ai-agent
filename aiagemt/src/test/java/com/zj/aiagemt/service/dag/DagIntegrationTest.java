package com.zj.aiagemt.service.dag;

import com.zj.aiagemt.model.entity.AiAgentVersion;
import com.zj.aiagemt.repository.base.AiAgentVersionMapper;
import com.zj.aiagemt.service.dag.context.DagExecutionContext;
import com.zj.aiagemt.service.dag.executor.DagExecutor;
import com.zj.aiagemt.service.dag.loader.DagLoaderService;
import com.zj.aiagemt.service.dag.model.DagGraph;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DAG集成测试
 * 使用真实的JSON数据和Spring上下文进行完整的端到端测试
 */
@Slf4j
@SpringBootTest
public class DagIntegrationTest {

    @Autowired
    private DagLoaderService dagLoaderService;

    @Autowired
    private DagExecutor dagExecutor;

    @Autowired
    private AiAgentVersionMapper agentVersionMapper;

    private Long testVersionId;

    /**
     * 准备测试数据
     */
    @BeforeEach
    void setUp() {
        // 创建测试用的graph_json
        String graphJson = createTestGraphJson();

        // 插入测试数据到数据库
        AiAgentVersion version = new AiAgentVersion();
        version.setAgentId(1L);
        version.setVersion("test-1.0");
        version.setGraphJson(graphJson);
        version.setStatus(1); // 已发布
        version.setCreateTime(LocalDateTime.now());
        version.setUpdateTime(LocalDateTime.now());

        agentVersionMapper.insert(version);
        testVersionId = version.getId();

        log.info("测试数据准备完成，versionId: {}", testVersionId);
    }

    /**
     * 测试完整的DAG执行流程
     */
    @Test
    void testFullDagExecution() {
        // 1. 加载DAG
        DagGraph dagGraph = dagLoaderService.loadDagByVersionId(testVersionId);

        assertNotNull(dagGraph, "DAG图不应为null");
        assertEquals("test-dag", dagGraph.getDagId());
        assertEquals(2, dagGraph.getNodes().size(), "应该有3个节点");

        log.info("DAG加载成功: {}", dagGraph.getDagId());

        // 2. 创建执行上下文
        String conversationId = "test-conversation-" + System.currentTimeMillis();
        DagExecutionContext context = new DagExecutionContext(conversationId);
        context.setValue("userInput", "请帮我分析这段代码的性能问题");
        context.setValue("userMessage", "请帮我分析这段代码的性能问题");

        // 3. 执行DAG
        DagExecutor.DagExecutionResult result = dagExecutor.execute(dagGraph, context);
        System.out.println(result);
        // 4. 验证结果
        assertNotNull(result, "执行结果不应为null");
        assertEquals("SUCCESS", result.getStatus(), "执行应该成功");
        assertTrue(result.getDurationMs() > 0, "执行耗时应该大于0");

        log.info("DAG执行完成: status={}, duration={}ms",
                result.getStatus(), result.getDurationMs());

        // 5. 验证节点执行结果
        Object planResult = context.getNodeResult("plan-node-1");
        assertNotNull(planResult, "规划节点应该有执行结果");

        Object actResult = context.getNodeResult("act-node-1");
        assertNotNull(actResult, "执行节点应该有执行结果");

        // 6. 验证context数据
        assertTrue(context.isNodeExecuted("plan-node-1"), "规划节点应该已执行");
        assertTrue(context.isNodeExecuted("act-node-1"), "执行节点应该已执行");

        log.info("所有验证通过！");
    }

    /**
     * 测试DAG加载功能
     */
    @Test
    void testDagLoading() {
        DagGraph dagGraph = dagLoaderService.loadDagByVersionId(testVersionId);

        assertNotNull(dagGraph);
        assertEquals("test-dag", dagGraph.getDagId());
        assertEquals("1.0", dagGraph.getVersion());

        // 验证节点
        assertNotNull(dagGraph.getNode("plan-node-1"));
        assertNotNull(dagGraph.getNode("act-node-1"));
        assertNotNull(dagGraph.getNode("end-node-1"));

        // 验证依赖关系
        assertEquals(0, dagGraph.getNodeDependencies("plan-node-1").size(),
                "规划节点应该没有依赖");
        assertEquals(1, dagGraph.getNodeDependencies("act-node-1").size(),
                "执行节点应该依赖1个节点");

        log.info("DAG加载测试通过");
    }

    /**
     * 测试拓扑排序
     */
    @Test
    void testTopologicalSort() {
        DagGraph dagGraph = dagLoaderService.loadDagByVersionId(testVersionId);

        var sortedNodes = com.zj.aiagemt.service.dag.executor.DagTopologicalSorter.sort(dagGraph);

        assertNotNull(sortedNodes);
        assertEquals(2, sortedNodes.size());

        // 验证顺序：plan-node-1 应该在 act-node-1 之前
        int planIndex = sortedNodes.indexOf("plan-node-1");
        int actIndex = sortedNodes.indexOf("act-node-1");
        assertTrue(planIndex < actIndex, "规划节点应该在执行节点之前");

        log.info("拓扑排序测试通过，顺序: {}", sortedNodes);
    }

    /**
     * 创建测试用的Graph JSON
     * 这是一个简单的3节点DAG：Plan -> Act -> End
     */
    private String createTestGraphJson() {
        return """
{
  "dagId": "test-dag",
  "version": "1.0",
  "description": "测试DAG工作流",
  "nodes": [
    {
      "nodeId": "plan-node-1",
      "nodeType": "PLAN",
      "nodeName": "任务规划",
      "position": { "x": 100, "y": 100 },
      "config": {
        "systemPrompt": "你是一个任务规划专家。请分析用户的需求，制定详细的执行计划。",
        "model": {
          "baseUrl": "https://globalai.vip",
          "apiKey": "sk-M5krIRHhrlTXuR409xwhJilDom8o3Cu6lf4x3HKvvDwBUR7l",
          "modelName": "gemini-2.5-flash-thinking",
          "temperature": 0.7,
          "maxTokens": 1000
        },
        "memory": {
          "enabled": false
        },
        "advisors": [],
        "mcpTools": [],
        "timeout": 30000
      }
    },
    {
      "nodeId": "act-node-1",
      "nodeType": "ACT",
      "nodeName": "任务执行",
      "position": { "x": 300, "y": 100 },
      "config": {
        "systemPrompt": "你是一个任务执行专家。请根据规划执行具体任务。",
        "model": {
          "baseUrl": "https://globalai.vip",
          "apiKey": "sk-M5krIRHhrlTXuR409xwhJilDom8o3Cu6lf4x3HKvvDwBUR7l",
          "modelName": "gemini-2.5-flash-thinking",
          "temperature": 0.3,
          "maxTokens": 2000
        },
        "memory": {
          "enabled": false
        },
        "advisors": [],
        "mcpTools": [],
        "timeout": 60000
      }
    }
  ],
  "edges": [
    {
      "edgeId": "edge-1",
      "source": "plan-node-1",
      "target": "act-node-1",
      "label": "规划完成"
    }
  ],
  "startNodeId": "plan-node-1"
}

                """;
    }
}
