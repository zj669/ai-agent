package com.zj.aiagent.infrastructure.workflow.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Edge;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.valobj.*;
import com.zj.aiagent.infrastructure.workflow.graph.converter.NodeConfigConverter;
import com.zj.aiagent.infrastructure.workflow.graph.dto.*;
import com.zj.aiagent.infrastructure.workflow.util.SpelToConditionConverter;
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
public class WorkflowGraphFactoryImpl {

    private final ObjectMapper objectMapper;
    private final NodeConfigConverter nodeConfigConverter;

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

            // 4. 构建边详情（包含条件表达式）
            Map<String, List<Edge>> edgeDetails = buildEdgeDetails(dto.getEdges());

            // 5. 构建领域对象
            return WorkflowGraph.builder()
                    .graphId(dto.getDagId())
                    .version(dto.getVersion())
                    .description(dto.getDescription())
                    .nodes(nodes)
                    .edges(edges)
                    .edgeDetails(edgeDetails)
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
     * 将 sourceRef 路径映射为 SpEL 表达式
     *
     * 映射规则：
     * - "nodeId.output.key"  → "#{#nodeId['key']}"
     * - "state.key"          → "#{#sharedState['key']}"
     * - "nodeId.key"         → "#{#nodeId['key']}"
     *
     * 格式不匹配时记录 WARN 日志并返回原始 sourceRef
     *
     * @param sourceRef 前端配置的数据来源路径
     * @return SpEL 表达式字符串，或原始 sourceRef（格式不匹配时）
     */
    static String mapSourceRefToSpEL(String sourceRef) {
        if (sourceRef == null || sourceRef.isBlank()) {
            return sourceRef;
        }

        int firstDot = sourceRef.indexOf('.');
        if (firstDot < 0 || firstDot == sourceRef.length() - 1) {
            // 没有 dot 或 dot 在末尾，格式不匹配
            log.warn("[GraphFactory] sourceRef 格式不匹配, 无法映射为 SpEL: sourceRef={}", sourceRef);
            return sourceRef;
        }

        String firstSegment = sourceRef.substring(0, firstDot);
        String rest = sourceRef.substring(firstDot + 1);

        // 格式: state.<key>
        if ("state".equals(firstSegment)) {
            return "#{#sharedState['" + rest + "']}";
        }

        // 格式: <nodeId>.output.<key>
        int secondDot = rest.indexOf('.');
        if (secondDot >= 0 && "output".equals(rest.substring(0, secondDot))) {
            String key = rest.substring(secondDot + 1);
            if (key.isEmpty()) {
                log.warn("[GraphFactory] sourceRef 格式不匹配, output 后缺少 key: sourceRef={}", sourceRef);
                return sourceRef;
            }
            // 对包含特殊字符的 nodeId 使用 nodeOutputs map 语法
            if (firstSegment.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                return "#{#" + firstSegment + "['" + key + "']}";
            }
            return "#{#nodeOutputs['" + firstSegment + "']['" + key + "']}";
        }

        // 格式: <nodeId>.<key>
        if (firstSegment.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            return "#{#" + firstSegment + "['" + rest + "']}";
        }
        return "#{#nodeOutputs['" + firstSegment + "']['" + rest + "']}";
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
                // 使用 sourceRef 映射为 SpEL 表达式
                inputs.put(field.getKey(), mapSourceRefToSpEL(field.getSourceRef()));
            } else if (field.getDefaultValue() != null) {
                // 使用默认值
                inputs.put(field.getKey(), field.getDefaultValue());
            }
        }
        return inputs;
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

    /**
     * 构建边详情映射（包含条件表达式）
     */
    private Map<String, List<Edge>> buildEdgeDetails(List<EdgeJsonDTO> edgeDTOs) {
        if (edgeDTOs == null || edgeDTOs.isEmpty()) {
            return new HashMap<>();
        }

        return edgeDTOs.stream()
                .filter(dto -> dto.getSource() != null && dto.getTarget() != null)
                .map(this::convertEdge)
                .collect(Collectors.groupingBy(Edge::getSource));
    }

    /**
     * 转换单个边
     */
    private Edge convertEdge(EdgeJsonDTO dto) {
        Edge.EdgeType edgeType = parseEdgeType(dto.getEdgeType());
        return Edge.builder()
                .edgeId(dto.getEdgeId())
                .source(dto.getSource())
                .target(dto.getTarget())
                .condition(dto.getCondition())
                .edgeType(edgeType)
                .build();
    }

    /**
     * 解析边类型
     */
    private Edge.EdgeType parseEdgeType(String edgeType) {
        if (edgeType == null || edgeType.isBlank()) {
            return Edge.EdgeType.DEPENDENCY;
        }
        try {
            return Edge.EdgeType.valueOf(edgeType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Edge.EdgeType.DEPENDENCY;
        }
    }

    // ========== 旧模型兼容转换 (Legacy Edge → ConditionBranch) ==========

    /**
     * 将旧模型 Edge 列表转换为新模型 ConditionBranch 列表
     * <p>
     * 转换规则：
     * - DEFAULT 边 → default 分支（priority = Integer.MAX_VALUE）
     * - CONDITIONAL 边 → 尝试解析 SpEL 为 ConditionItem，成功则创建非 default 分支
     * - 无法解析的 SpEL → 作为 default 处理并 log warn
     *
     * @param edges 旧模型边列表
     * @return 转换后的 ConditionBranch 列表
     */
    List<ConditionBranch> convertLegacyEdgesToBranches(List<Edge> edges) {
        List<ConditionBranch> branches = new ArrayList<>();
        int priority = 0;

        for (Edge edge : edges) {
            if (edge.isDefault()) {
                branches.add(ConditionBranch.builder()
                        .priority(Integer.MAX_VALUE)
                        .targetNodeId(edge.getTarget())
                        .isDefault(true)
                        .conditionGroups(List.of())
                        .build());
            } else {
                // 尝试将 SpEL 表达式解析为 ConditionItem
                ConditionItem item = SpelToConditionConverter.parse(edge.getCondition());
                if (item != null) {
                    branches.add(ConditionBranch.builder()
                            .priority(priority++)
                            .targetNodeId(edge.getTarget())
                            .isDefault(false)
                            .conditionGroups(List.of(
                                    ConditionGroup.builder()
                                            .operator(LogicalOperator.AND)
                                            .conditions(List.of(item))
                                            .build()))
                            .build());
                } else {
                    // 无法解析，作为 default 处理
                    log.warn("[GraphFactory] 无法解析旧条件表达式: {}, 作为 default 处理", edge.getCondition());
                    branches.add(ConditionBranch.builder()
                            .priority(Integer.MAX_VALUE)
                            .targetNodeId(edge.getTarget())
                            .isDefault(true)
                            .conditionGroups(List.of())
                            .build());
                }
            }
        }
        return branches;
    }

    /**
     * 尝试将旧 SpEL 表达式解析为 ConditionItem
     */
    ConditionItem parseLegacySpelToItem(String spelExpression) {
        return SpelToConditionConverter.parse(spelExpression);
    }
}
