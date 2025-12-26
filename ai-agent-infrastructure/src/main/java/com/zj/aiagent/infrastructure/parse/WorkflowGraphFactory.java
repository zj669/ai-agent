package com.zj.aiagent.infrastructure.parse;

import com.alibaba.fastjson2.JSON;
import com.zj.aiagent.domain.agent.dag.entity.GraphJsonSchema;
import com.zj.aiagent.domain.agent.dag.exception.NodeConfigException;
import com.zj.aiagent.domain.toolbox.parse.AgentConfigParseFactory;
import com.zj.aiagent.domain.workflow.entity.EdgeDefinitionEntity;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;
import com.zj.aiagent.infrastructure.persistence.repository.IAiAgentRepository;
import com.zj.aiagent.shared.design.workflow.NodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Repository
public class WorkflowGraphFactory {
    private final IAiAgentRepository agentRepository;
    private final AgentConfigParseFactory agentConfigParseFactory;

    public WorkflowGraph loadDagByAgentId(String agentId){
        AiAgentPO aiAgent = agentRepository.getById(agentId);
        if (aiAgent == null) {
            throw new NodeConfigException("Agent not found: agentId=" + agentId);
        }

        if (aiAgent.getGraphJson() == null || aiAgent.getGraphJson().isEmpty()) {
            throw new NodeConfigException("Agent graph_json is empty: agentId=" + agentId);
        }

        return loadDagFromJson(aiAgent.getGraphJson());
    }


    public WorkflowGraph loadDagFromJson(String graphJson) {
        try {
            log.info("开始解析graph_json");

            // 解析JSON
            GraphJsonSchema schema = JSON.parseObject(graphJson, GraphJsonSchema.class);

            if (schema == null || schema.getNodes() == null || schema.getNodes().isEmpty()) {
                throw new NodeConfigException("Invalid graph_json: nodes cannot be empty");
            }

            // 构建节点映射（统一使用 AbstractConfigurableNode 类型）
            Map<String, NodeExecutor> nodeMap = new HashMap<>();
            for (GraphJsonSchema.NodeDefinition nodeDef : schema.getNodes()) {
                NodeExecutor node = agentConfigParseFactory.createNode(nodeDef);
                nodeMap.put(nodeDef.getNodeId(), node);
                log.info("创建节点: {} ({})", nodeDef.getNodeName(), nodeDef.getNodeId());
            }

            // 构建依赖关系
            Map<String, List<String>> dependencies = buildDependencies(schema.getEdges());

            // 构建DAG图
            WorkflowGraph dagGraph = WorkflowGraph.builder()
                    .dagId(schema.getDagId())
                    .nodes(nodeMap)
                    .edges(convert(schema.getEdges()))
                    .dependencies(dependencies)
                    .startNodeId(schema.getStartNodeId())
                    .build();

            log.info("DAG加载完成: dagId={}, 节点数={}", schema.getDagId(), nodeMap.size());

            return dagGraph;

        } catch (Exception e) {
            throw new NodeConfigException("Failed to load DAG from JSON: " + e.getMessage(), e);
        }
    }

    private List<EdgeDefinitionEntity> convert(List<GraphJsonSchema.EdgeDefinition> edges){
        List<EdgeDefinitionEntity> edgeDefinitionEntities = new ArrayList<>();
        for (GraphJsonSchema.EdgeDefinition edge : edges) {
            edgeDefinitionEntities.add(new EdgeDefinitionEntity(edge.getEdgeId(), edge.getSource(), edge.getTarget(),
                    edge.getCondition(), edge.getEdgeType()));
        }
        return edgeDefinitionEntities;
    }

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
