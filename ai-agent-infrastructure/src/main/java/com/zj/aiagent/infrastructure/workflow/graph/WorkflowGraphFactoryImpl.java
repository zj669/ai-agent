package com.zj.aiagent.infrastructure.workflow.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.service.WorkflowGraphFactory;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import com.zj.aiagent.infrastructure.workflow.graph.converter.NodeConfigConverter;
import com.zj.aiagent.infrastructure.workflow.graph.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * WorkflowGraphFactory 实现类
 * 负责将 graphJson 字符串解析为 WorkflowGraph 领域对象
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowGraphFactoryImpl implements WorkflowGraphFactory {

    private final ObjectMapper objectMapper;
    private final NodeConfigConverter nodeConfigConverter;

    @Override
    public WorkflowGraph fromJson(String graphJson) {
        if (graphJson == null || graphJson.isBlank()) {
            throw new IllegalArgumentException("graphJson cannot be null or blank");
        }

        try {
            // 1. 解析顶层结构
            GraphJsonDTO dto = objectMapper.readValue(graphJson, GraphJsonDTO.class);

            // 2. 转换节点
            Map<String, Node> nodes = convertNodes(dto.getNodes());

            // 3. 转换边为邻接表
            Map<String, Set<String>> edges = buildAdjacencyList(dto.getEdges());

            // 4. 构建领域对象
            return WorkflowGraph.builder()
                    .graphId(dto.getDagId())
                    .version(dto.getVersion())
                    .description(dto.getDescription())
                    .nodes(nodes)
                    .edges(edges)
                    .build();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GraphFactory] Failed to parse graphJson: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid graphJson format: " + e.getMessage(), e);
        }
    }

    /**
     * 转换节点列表为 Map
     */
    private Map<String, Node> convertNodes(List<NodeJsonDTO> nodeDTOs) {
        if (nodeDTOs == null || nodeDTOs.isEmpty()) {
            return new HashMap<>();
        }

        return nodeDTOs.stream()
                .map(this::convertNode)
                .collect(Collectors.toMap(Node::getNodeId, node -> node, (a, b) -> a));
    }

    /**
     * 转换单个节点
     */
    private Node convertNode(NodeJsonDTO dto) {
        NodeType type = parseNodeType(dto.getNodeType());
        NodeConfig config = nodeConfigConverter.convert(type, dto.getUserConfig());

        return Node.builder()
                .nodeId(dto.getNodeId())
                .name(dto.getNodeName())
                .type(type)
                .config(config)
                .inputs(extractInputs(dto.getInputSchema()))
                .outputs(extractOutputs(dto.getOutputSchema()))
                .position(convertPosition(dto.getPosition()))
                .build();
    }

    /**
     * 解析节点类型
     */
    private NodeType parseNodeType(String nodeType) {
        if (nodeType == null || nodeType.isBlank()) {
            throw new IllegalArgumentException("nodeType cannot be null or blank");
        }
        try {
            return NodeType.valueOf(nodeType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[GraphFactory] Unknown nodeType: {}, defaulting to LLM", nodeType);
            return NodeType.LLM;
        }
    }

    /**
     * 从 inputSchema 提取输入映射
     * key: 字段键名, value: sourceRef（数据来源）或默认值
     */
    private Map<String, Object> extractInputs(List<FieldSchemaDTO> inputSchema) {
        if (inputSchema == null || inputSchema.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> inputs = new HashMap<>();
        for (FieldSchemaDTO field : inputSchema) {
            if (field.getSourceRef() != null && !field.getSourceRef().isBlank()) {
                // 使用 sourceRef 表达式
                inputs.put(field.getKey(), "${" + field.getSourceRef() + "}");
            } else if (field.getDefaultValue() != null) {
                // 使用默认值
                inputs.put(field.getKey(), field.getDefaultValue());
            }
        }
        return inputs;
    }

    /**
     * 从 outputSchema 提取输出映射
     * key: 上下文中的 key, value: 结果提取路径（暂时使用 key 本身）
     */
    private Map<String, String> extractOutputs(List<FieldSchemaDTO> outputSchema) {
        if (outputSchema == null || outputSchema.isEmpty()) {
            return new HashMap<>();
        }

        return outputSchema.stream()
                .collect(Collectors.toMap(
                        FieldSchemaDTO::getKey,
                        field -> field.getKey(), // 暂时使用 key 作为提取路径
                        (a, b) -> a));
    }

    /**
     * 转换位置
     */
    private Node.Position convertPosition(PositionDTO positionDTO) {
        if (positionDTO == null) {
            return null;
        }
        return Node.Position.builder()
                .x(positionDTO.getX() != null ? positionDTO.getX() : 0.0)
                .y(positionDTO.getY() != null ? positionDTO.getY() : 0.0)
                .build();
    }

    /**
     * 将边列表转换为邻接表
     * 返回: sourceNodeId -> Set<targetNodeId>
     */
    private Map<String, Set<String>> buildAdjacencyList(List<EdgeJsonDTO> edgeDTOs) {
        if (edgeDTOs == null || edgeDTOs.isEmpty()) {
            return new HashMap<>();
        }

        return edgeDTOs.stream()
                .filter(edge -> edge.getSource() != null && edge.getTarget() != null)
                .collect(Collectors.groupingBy(
                        EdgeJsonDTO::getSource,
                        Collectors.mapping(EdgeJsonDTO::getTarget, Collectors.toSet())));
    }
}
