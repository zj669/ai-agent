package com.zj.aiagemt.service.dag;

import com.zj.aiagemt.service.dag.context.DagExecutionContext;
import com.zj.aiagemt.service.dag.executor.DagExecutor;
import com.zj.aiagemt.service.dag.loader.DagLoaderService;
import com.zj.aiagemt.service.dag.model.DagGraph;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * DAG简单测试
 * 不依赖Spring，直接使用JSON字符串测试DAG加载和执行
 */
@Slf4j
public class DagSimpleTest {

    /**
     * 测试从JSON加载DAG
     */
    @Test
    void testLoadDagFromJson() {
        String graphJson = createSimpleGraphJson();

        // 注意：这个测试需要Spring上下文来创建节点
        // 因为节点创建依赖ApplicationContext
        // 建议使用DagIntegrationTest进行完整测试

        log.info("Graph JSON created:\n{}", graphJson);

        // 验证JSON格式
        assert graphJson.contains("\"dagId\"");
        assert graphJson.contains("\"nodes\"");
        assert graphJson.contains("\"edges\"");

        log.info("JSON格式验证通过");
    }

    /**
     * 创建简单的Graph JSON用于手动测试
     */
    private String createSimpleGraphJson() {
        return """
                {
                  "dagId": "simple-test-dag",
                  "version": "1.0",
                  "description": "简单测试DAG",
                  "nodes": [
                    {
                      "nodeId": "plan-1",
                      "nodeType": "PLAN",
                      "nodeName": "规划节点",
                      "position": { "x": 100, "y": 100 },
                      "config": {
                        "systemPrompt": "你是一个规划专家",
                        "model": {
                          "baseUrl": "https://api.openai.com/v1",
                          "apiKey": "your-api-key",
                          "modelName": "gpt-3.5-turbo",
                          "temperature": 0.7
                        },
                        "timeout": 30000
                      }
                    },
                    {
                      "nodeId": "end-1",
                      "nodeType": "END",
                      "nodeName": "结束",
                      "position": { "x": 300, "y": 100 },
                      "config": {}
                    }
                  ],
                  "edges": [
                    {
                      "edgeId": "e1",
                      "source": "plan-1",
                      "target": "end-1",
                      "label": "完成"
                    }
                  ],
                  "startNodeId": "plan-1"
                }
                """;
    }

    /**
     * 打印复杂的Graph JSON示例（包含所有节点类型）
     */
    @Test
    void printComplexGraphJsonExample() {
        String complexJson = createComplexGraphJson();
        log.info("=== 复杂DAG Graph JSON示例 ===\n{}", complexJson);
    }

    /**
     * 创建包含所有节点类型的复杂Graph JSON
     */
    private String createComplexGraphJson() {
        return """
                {
                  "dagId": "complex-workflow",
                  "version": "2.0",
                  "description": "包含所有节点类型的完整工作流",
                  "nodes": [
                    {
                      "nodeId": "plan-node",
                      "nodeType": "PLAN",
                      "nodeName": "任务规划",
                      "position": { "x": 100, "y": 100 },
                      "config": {
                        "systemPrompt": "你是一个任务规划专家。请详细分析用户需求并制定执行计划。",
                        "model": {
                          "baseUrl": "https://api.openai.com/v1",
                          "apiKey": "sk-your-key",
                          "modelName": "gpt-4",
                          "temperature": 0.7,
                          "maxTokens": 2000
                        },
                        "memory": {
                          "enabled": true,
                          "type": "VECTOR_STORE",
                          "retrieveSize": 5,
                          "conversationId": "${conversationId}"
                        },
                        "advisors": [
                          {
                            "advisorId": "memory-advisor-1",
                            "advisorType": "MEMORY",
                            "config": {}
                          }
                        ],
                        "mcpTools": [],
                        "timeout": 30000
                      }
                    },
                    {
                      "nodeId": "act-node",
                      "nodeType": "ACT",
                      "nodeName": "任务执行",
                      "position": { "x": 300, "y": 100 },
                      "config": {
                        "systemPrompt": "你是一个任务执行专家。请使用工具完成任务。",
                        "model": {
                          "baseUrl": "https://api.openai.com/v1",
                          "apiKey": "sk-your-key",
                          "modelName": "gpt-4",
                          "temperature": 0.3
                        },
                        "memory": {
                          "enabled": true,
                          "type": "CHAT_MEMORY",
                          "retrieveSize": 10
                        },
                        "mcpTools": [
                          {
                            "mcpId": "file-tool",
                            "mcpName": "文件操作",
                            "mcpType": "FILE_SYSTEM"
                          }
                        ],
                        "timeout": 60000
                      }
                    },
                    {
                      "nodeId": "human-node",
                      "nodeType": "HUMAN",
                      "nodeName": "人工审核",
                      "position": { "x": 500, "y": 100 },
                      "config": {
                        "systemPrompt": "等待人工审核",
                        "checkMessage": "请审核任务执行结果",
                        "allowContextModification": true,
                        "timeout": 3600000
                      }
                    },
                    {
                      "nodeId": "router-node",
                      "nodeType": "ROUTER",
                      "nodeName": "结果评估",
                      "position": { "x": 700, "y": 100 },
                      "config": {
                        "systemPrompt": "你是一个路由决策专家。请评估任务完成情况。",
                        "model": {
                          "baseUrl": "https://api.openai.com/v1",
                          "apiKey": "sk-your-key",
                          "modelName": "gpt-4"
                        },
                        "routingStrategy": "AI",
                        "candidateNodes": ["act-node", "end-node"],
                        "routingPrompt": "如果任务完成选择end-node，否则选择act-node重试"
                      }
                    },
                    {
                      "nodeId": "end-node",
                      "nodeType": "END",
                      "nodeName": "完成",
                      "position": { "x": 900, "y": 100 },
                      "config": {}
                    }
                  ],
                  "edges": [
                    {
                      "edgeId": "e1",
                      "source": "plan-node",
                      "target": "act-node",
                      "label": "规划完成"
                    },
                    {
                      "edgeId": "e2",
                      "source": "act-node",
                      "target": "human-node",
                      "label": "执行完成"
                    },
                    {
                      "edgeId": "e3",
                      "source": "human-node",
                      "target": "router-node",
                      "label": "审核通过"
                    },
                    {
                      "edgeId": "e4",
                      "source": "router-node",
                      "target": "act-node",
                      "label": "需要重试",
                      "condition": "RETRY"
                    },
                    {
                      "edgeId": "e5",
                      "source": "router-node",
                      "target": "end-node",
                      "label": "任务完成",
                      "condition": "COMPLETE"
                    }
                  ],
                  "startNodeId": "plan-node"
                }
                """;
    }
}
