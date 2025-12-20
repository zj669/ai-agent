package com.zj.aiagemt.service.dag.loader;

import com.alibaba.fastjson2.JSON;
import com.zj.aiagemt.model.entity.AiAgent;
import com.zj.aiagemt.repository.base.AiAgentMapper;
import com.zj.aiagemt.service.dag.exception.NodeConfigException;
import com.zj.aiagemt.service.dag.factory.NodeFactory;
import com.zj.aiagemt.service.dag.model.DagGraph;
import com.zj.aiagemt.service.dag.model.GraphJsonSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAG加载服务 - 从数据库加载DAG配置并构建节点
 */
@Slf4j
@Service
public class DagLoaderService {

    private final AiAgentMapper aiAgentMapper;
    private final NodeFactory nodeFactory;

    public DagLoaderService(AiAgentMapper aiAgentMapper, NodeFactory nodeFactory) {
        this.aiAgentMapper = aiAgentMapper;
        this.nodeFactory = nodeFactory;
    }

    /**
     * 根据agentId加载DAG（新版：直接从ai_agent表加载）
     */
    public DagGraph loadDagByAgentId(String agentId) {
        // 从数据库查询
        AiAgent aiAgent = aiAgentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiAgent>()
                        .eq(AiAgent::getAgentId, agentId));

        if (aiAgent == null) {
            throw new NodeConfigException("Agent not found: agentId=" + agentId);
        }

        if (aiAgent.getGraphJson() == null || aiAgent.getGraphJson().isEmpty()) {
            throw new NodeConfigException("Agent graph_json is empty: agentId=" + agentId);
        }

        return loadDagFromJson(aiAgent.getGraphJson());
    }

    /**
     * 根据主键ID加载DAG
     */
    public DagGraph loadDagById(Long id) {
        AiAgent aiAgent = aiAgentMapper.selectById(id);

        if (aiAgent == null) {
            throw new NodeConfigException("Agent not found: id=" + id);
        }

        if (aiAgent.getGraphJson() == null || aiAgent.getGraphJson().isEmpty()) {
            throw new NodeConfigException("Agent graph_json is empty: id=" + id);
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

            // 构建节点映射（使用Object类型支持ConditionalDagNode）
            Map<String, Object> nodeMap = new HashMap<>();
            for (GraphJsonSchema.NodeDefinition nodeDef : schema.getNodes()) {
                Object node = nodeFactory.createNode(nodeDef);
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
