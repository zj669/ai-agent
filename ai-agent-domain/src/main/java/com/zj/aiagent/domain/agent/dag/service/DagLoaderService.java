package com.zj.aiagent.domain.agent.dag.service;

import com.alibaba.fastjson2.JSON;
import com.zj.aiagent.domain.agent.dag.entity.AiAgent;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.entity.GraphJsonSchema;
import com.zj.aiagent.domain.agent.dag.exception.NodeConfigException;
import com.zj.aiagent.domain.agent.dag.factory.NodeFactory;
import com.zj.aiagent.domain.agent.dag.node.AbstractConfigurableNode;
import com.zj.aiagent.domain.agent.dag.repository.IDagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DagLoaderService {
    private final IDagRepository dagRepository;
    private final NodeFactory nodeFactory;

    /**
     * 根据agentId加载DAG（新版：直接从ai_agent表加载）
     */
    public DagGraph loadDagByAgentId(String agentId) {
        // 从数据库查询
        AiAgent aiAgent = dagRepository.selectAiAgentByAgentId(agentId);

        if (aiAgent == null) {
            throw new NodeConfigException("Agent not found: agentId=" + agentId);
        }

        if (aiAgent.getGraphJson() == null || aiAgent.getGraphJson().isEmpty()) {
            throw new NodeConfigException("Agent graph_json is empty: agentId=" + agentId);
        }

        return loadDagFromJson(aiAgent.getGraphJson());
    }

    /**
     * 从JSON字符串加载DAG
     */
    public DagGraph loadDagFromJson(String graphJson) {
        try {
            log.info("开始解析graph_json");

            // 解析JSON
            GraphJsonSchema schema = JSON.parseObject(graphJson, GraphJsonSchema.class);

            if (schema == null || schema.getNodes() == null || schema.getNodes().isEmpty()) {
                throw new NodeConfigException("Invalid graph_json: nodes cannot be empty");
            }

            // 构建节点映射（统一使用 AbstractConfigurableNode 类型）
            Map<String, AbstractConfigurableNode> nodeMap = new HashMap<>();
            for (GraphJsonSchema.NodeDefinition nodeDef : schema.getNodes()) {
                AbstractConfigurableNode node = nodeFactory.createNode(nodeDef, schema.getEdges());
                nodeMap.put(nodeDef.getNodeId(), node);
                log.info("创建节点: {} ({})", nodeDef.getNodeName(), nodeDef.getNodeId());
            }

            // 构建依赖关系
            Map<String, List<String>> dependencies = buildDependencies(schema.getEdges());

            // 构建DAG图
            DagGraph dagGraph = DagGraph.builder()
                    .dagId(schema.getDagId())
                    .version(schema.getVersion())
                    .description(schema.getDescription())
                    .nodes(nodeMap)
                    .edges(schema.getEdges())
                    .dependencies(dependencies)
                    .startNodeId(schema.getStartNodeId())
                    .build();

            log.info("DAG加载完成: dagId={}, 节点数={}", schema.getDagId(), nodeMap.size());

            return dagGraph;

        } catch (Exception e) {
            throw new NodeConfigException("Failed to load DAG from JSON: " + e.getMessage(), e);
        }
    }

    /**
     * 构建依赖关系映射
     */
    private Map<String, List<String>> buildDependencies(List<GraphJsonSchema.EdgeDefinition> edges) {
        Map<String, List<String>> dependencies = new HashMap<>();

        if (edges != null) {
            for (GraphJsonSchema.EdgeDefinition edge : edges) {
                dependencies.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>())
                        .add(edge.getSource());
            }
        }

        return dependencies;
    }
}