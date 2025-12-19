package com.zj.aiagemt.service.dag.loader;

import com.alibaba.fastjson2.JSON;
import com.zj.aiagemt.common.design.dag.DagNode;
import com.zj.aiagemt.model.entity.AiAgentVersion;
import com.zj.aiagemt.repository.base.AiAgentVersionMapper;
import com.zj.aiagemt.service.dag.context.DagExecutionContext;
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

    private final AiAgentVersionMapper agentVersionMapper;
    private final NodeFactory nodeFactory;

    public DagLoaderService(AiAgentVersionMapper agentVersionMapper, NodeFactory nodeFactory) {
        this.agentVersionMapper = agentVersionMapper;
        this.nodeFactory = nodeFactory;
    }

    /**
     * 根据agentId和version加载DAG
     */
    public DagGraph loadDagByAgentIdAndVersion(Long agentId, String version) {
        // 从数据库查询
        AiAgentVersion agentVersion = agentVersionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiAgentVersion>()
                        .eq(AiAgentVersion::getAgentId, agentId)
                        .eq(AiAgentVersion::getVersion, version)
                        .eq(AiAgentVersion::getStatus, 1) // 已发布状态
        );

        if (agentVersion == null) {
            throw new NodeConfigException("Agent version not found: agentId=" + agentId + ", version=" + version);
        }

        return loadDagFromJson(agentVersion.getGraphJson());
    }

    /**
     * 根据versionId加载DAG
     */
    public DagGraph loadDagByVersionId(Long versionId) {
        AiAgentVersion agentVersion = agentVersionMapper.selectById(versionId);

        if (agentVersion == null) {
            throw new NodeConfigException("Agent version not found: versionId=" + versionId);
        }

        return loadDagFromJson(agentVersion.getGraphJson());
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

            // 构建节点映射
            Map<String, DagNode<DagExecutionContext, String>> nodeMap = new HashMap<>();
            for (GraphJsonSchema.NodeDefinition nodeDef : schema.getNodes()) {
                DagNode<DagExecutionContext, String> node = nodeFactory.createNode(nodeDef);
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
