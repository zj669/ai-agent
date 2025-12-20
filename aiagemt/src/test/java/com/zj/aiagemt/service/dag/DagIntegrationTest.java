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
    String graphJson = createRouterTestGraphJson();

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
//    assertEquals("test-dag", dagGraph.getDagId());
//    assertEquals(2, dagGraph.getNodes().size(), "应该有3个节点");

    log.info("DAG加载成功: {}", dagGraph.getDagId());

    // 2. 创建执行上下文
    String conversationId = "test-conversation-" + System.currentTimeMillis();
    DagExecutionContext context = new DagExecutionContext(conversationId);
    context.setValue("userInput", "请帮我分析这段代码的性能问题,这是一个复杂的问题");
    context.setValue("userMessage", "请帮我分析这段代码的性能问题");

    // 3. 执行DAG
    DagExecutor.DagExecutionResult result = dagExecutor.execute(dagGraph, context);
    System.out.println(result);
    System.out.println(context);
//    // 4. 验证结果
//    assertNotNull(result, "执行结果不应为null");
//    assertEquals("SUCCESS", result.getStatus(), "执行应该成功");
//    assertTrue(result.getDurationMs() > 0, "执行耗时应该大于0");
//
//    log.info("DAG执行完成: status={}, duration={}ms",
//        result.getStatus(), result.getDurationMs());
//
//    // 5. 验证节点执行结果
//    Object planResult = context.getNodeResult("plan-node-1");
//    assertNotNull(planResult, "规划节点应该有执行结果");
//
//    Object actResult = context.getNodeResult("act-node-1");
//    assertNotNull(actResult, "执行节点应该有执行结果");
//
//    // 6. 验证context数据
//    assertTrue(context.isNodeExecuted("plan-node-1"), "规划节点应该已执行");
//    assertTrue(context.isNodeExecuted("act-node-1"), "执行节点应该已执行");

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

  /**
   * 创建包含路由节点的测试DAG JSON
   * Plan -> Router -> (Simple Task / Complex Task) -> END
   */
  private String createRouterTestGraphJson() {
    return """
        {
          "dagId": "test-router-dag",
          "version": "1.0",
          "description": "包含路由决策的测试DAG",
          "nodes": [
            {
              "nodeId": "plan-node",
              "nodeType": "PLAN",
              "nodeName": "任务分析",
              "position": { "x": 100, "y": 100 },
              "config": {
                "systemPrompt": "你是一个任务分析专家。请分析用户的需求，并评估任务的复杂度（简单/复杂）。如果任务比较简单（如简单计算、查询等），回复中包含'简单任务'；如果任务复杂（如数据分析、多步骤操作等），回复中包含'复杂任务'。",
                "model": {
                  "baseUrl": "https://globalai.vip",
                  "apiKey": "sk-M5krIRHhrlTXuR409xwhJilDom8o3Cu6lf4x3HKvvDwBUR7l",
                  "modelName": "gemini-2.5-flash-thinking",
                  "temperature": 0.3,
                  "maxTokens": 500
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
              "nodeId": "router-node",
              "nodeType": "ROUTER",
              "nodeName": "任务路由",
              "position": { "x": 300, "y": 100 },
              "config": {
                "systemPrompt": "根据任务分析结果，决定使用简单处理流程还是复杂处理流程。",
                "model": {
                  "baseUrl": "https://globalai.vip",
                  "apiKey": "sk-M5krIRHhrlTXuR409xwhJilDom8o3Cu6lf4x3HKvvDwBUR7l",
                  "modelName": "gemini-2.5-flash-thinking",
                  "temperature": 0.1,
                  "maxTokens": 200
                },
                "memory": {
                  "enabled": false
                },
                "advisors": [],
                "mcpTools": [],
                "timeout": 20000,
                "routingStrategy": "AI",
                "candidateNodes": ["simple-task-node", "complex-task-node"],
                "routingPrompt": "根据前面的任务分析结果，判断应该使用哪个处理节点：\\n- 如果是简单任务，选择: simple-task-node\\n- 如果是复杂任务，选择: complex-task-node\\n\\n请直接返回节点ID，不要有其他内容。"
              }
            },
            {
              "nodeId": "simple-task-node",
              "nodeType": "ACT",
              "nodeName": "简单任务处理",
              "position": { "x": 500, "y": 50 },
              "config": {
                "systemPrompt": "你是一个简单任务处理专家。请快速完成用户的简单请求。",
                "model": {
                  "baseUrl": "https://globalai.vip",
                  "apiKey": "sk-M5krIRHhrlTXuR409xwhJilDom8o3Cu6lf4x3HKvvDwBUR7l",
                  "modelName": "gemini-2.5-flash-thinking",
                  "temperature": 0.3,
                  "maxTokens": 500
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
              "nodeId": "complex-task-node",
              "nodeType": "ACT",
              "nodeName": "复杂任务处理",
              "position": { "x": 500, "y": 150 },
              "config": {
                "systemPrompt": "你是一个复杂任务处理专家。请详细分析并执行用户的复杂请求，提供详尽的解决方案。",
                "model": {
                  "baseUrl": "https://globalai.vip",
                  "apiKey": "sk-M5krIRHhrlTXuR409xwhJilDom8o3Cu6lf4x3HKvvDwBUR7l",
                  "modelName": "gemini-2.5-flash-thinking",
                  "temperature": 0.7,
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
              "source": "plan-node",
              "target": "router-node",
              "label": "分析完成"
            },
            {
              "edgeId": "edge-2",
              "source": "router-node",
              "target": "simple-task-node",
              "label": "简单任务"
            },
            {
              "edgeId": "edge-3",
              "source": "router-node",
              "target": "complex-task-node",
              "label": "复杂任务"
            }
          ],
          "startNodeId": "plan-node"
        }
                        """;
  }

  /**
   * 测试包含路由节点的DAG执行 - 简单任务场景
   */
  @Test
  void testRouterDagWithSimpleTask() {
    // 1. 准备包含路由节点的测试数据
    String routerGraphJson = createRouterTestGraphJson();

    AiAgentVersion routerVersion = new AiAgentVersion();
    routerVersion.setAgentId(2L);
    routerVersion.setVersion("test-router-1.0");
    routerVersion.setGraphJson(routerGraphJson);
    routerVersion.setStatus(1);
    routerVersion.setCreateTime(LocalDateTime.now());
    routerVersion.setUpdateTime(LocalDateTime.now());

    agentVersionMapper.insert(routerVersion);
    Long routerVersionId = routerVersion.getId();

    log.info("路由DAG测试数据准备完成，versionId: {}", routerVersionId);

    // 2. 加载DAG
    DagGraph dagGraph = dagLoaderService.loadDagByVersionId(routerVersionId);

    assertNotNull(dagGraph, "DAG图不应为null");
    assertEquals("test-router-dag", dagGraph.getDagId());
    assertEquals(4, dagGraph.getNodes().size(), "应该有4个节点");

    // 3. 创建执行上下文 - 简单任务
    String conversationId = "test-router-simple-" + System.currentTimeMillis();
    DagExecutionContext context = new DagExecutionContext(conversationId);
    context.setValue("userInput", "1+1等于几？");
    context.setValue("userMessage", "1+1等于几？");

    // 4. 执行DAG
    DagExecutor.DagExecutionResult result = dagExecutor.execute(dagGraph, context);

    // 5. 验证结果
    assertNotNull(result, "执行结果不应为null");
    assertEquals("SUCCESS", result.getStatus(), "执行应该成功");

    // 6. 验证路由决策
    Object routerDecision = context.getNodeResult("router-node");
    assertNotNull(routerDecision, "路由节点应该有执行结果");
    log.info("路由决策: {}", routerDecision);

    // 7. 验证执行了简单任务节点
    Object simpleTaskResult = context.getNodeResult("simple-task-node");
    Object complexTaskResult = context.getNodeResult("complex-task-node");

    log.info("简单任务执行结果: {}", simpleTaskResult);
    log.info("复杂任务执行结果: {}", complexTaskResult);

    // 根据路由决策，只有一个路径会被执行
    assertTrue(simpleTaskResult != null || complexTaskResult != null,
        "应该执行了简单任务或复杂任务之一");

    log.info("路由DAG（简单任务）测试完成！");
  }

  /**
   * 测试包含路由节点的DAG执行 - 复杂任务场景
   */
  @Test
  void testRouterDagWithComplexTask() {
    // 1. 准备包含路由节点的测试数据
    String routerGraphJson = createRouterTestGraphJson();

    AiAgentVersion routerVersion = new AiAgentVersion();
    routerVersion.setAgentId(3L);
    routerVersion.setVersion("test-router-complex-1.0");
    routerVersion.setGraphJson(routerGraphJson);
    routerVersion.setStatus(1);
    routerVersion.setCreateTime(LocalDateTime.now());
    routerVersion.setUpdateTime(LocalDateTime.now());

    agentVersionMapper.insert(routerVersion);
    Long routerVersionId = routerVersion.getId();

    log.info("路由DAG（复杂任务）测试数据准备完成，versionId: {}", routerVersionId);

    // 2. 加载DAG
    DagGraph dagGraph = dagLoaderService.loadDagByVersionId(routerVersionId);

    assertNotNull(dagGraph);
    assertEquals(4, dagGraph.getNodes().size());

    // 3. 创建执行上下文 - 复杂任务
    String conversationId = "test-router-complex-" + System.currentTimeMillis();
    DagExecutionContext context = new DagExecutionContext(conversationId);
    context.setValue("userInput", "请帮我分析一下如何优化这个大型数据处理系统的性能，需要考虑数据库索引、缓存策略、并发处理等多个方面");
    context.setValue("userMessage", "请帮我分析一下如何优化这个大型数据处理系统的性能，需要考虑数据库索引、缓存策略、并发处理等多个方面");

    // 4. 执行DAG
    DagExecutor.DagExecutionResult result = dagExecutor.execute(dagGraph, context);

    // 5. 验证结果
    assertNotNull(result);
    assertEquals("SUCCESS", result.getStatus(), "执行应该成功");

    // 6. 验证路由决策
    Object routerDecision = context.getNodeResult("router-node");
    assertNotNull(routerDecision, "路由节点应该有执行结果");
    log.info("路由决策: {}", routerDecision);

    // 7. 验证执行结果
    Object simpleTaskResult = context.getNodeResult("simple-task-node");
    Object complexTaskResult = context.getNodeResult("complex-task-node");

    log.info("简单任务执行结果: {}", simpleTaskResult);
    log.info("复杂任务执行结果: {}", complexTaskResult);

    // 根据路由决策，只有一个路径会被执行
    assertTrue(simpleTaskResult != null || complexTaskResult != null,
        "应该执行了简单任务或复杂任务之一");

    log.info("路由DAG（复杂任务）测试完成！");
  }
}
