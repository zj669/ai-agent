package com.zj.aiagent.infrastructure.workflow.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Edge;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.service.WorkflowGraphFactory;
import com.zj.aiagent.domain.workflow.valobj.*;
import com.zj.aiagent.infrastructure.workflow.graph.converter.NodeConfigConverter;
import com.zj.aiagent.infrastructure.workflow.graph.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            return "#{#" + firstSegment + "['" + key + "']}";
        }

        // 格式: <nodeId>.<key>
        return "#{#" + firstSegment + "['" + rest + "']}";
    }

    /**
     * 将 SpEL 表达式还原为 sourceRef 路径（Pretty Printer）
     *
     * 逆向映射规则：
     * - "#{#sharedState['key']}" → "state.key"
     * - "#{#nodeId['key']}"     → "nodeId.output.key"
     *
     * 格式不匹配时返回原始 SpEL 表达式
     *
     * @param spel SpEL 表达式
     * @return sourceRef 路径，或原始 spel（格式不匹配时）
     */
    static String mapSpELToSourceRef(String spel) {
        if (spel == null || spel.isBlank()) {
            return spel;
        }

        // 检查 #{...} 包裹格式
        if (!spel.startsWith("#{") || !spel.endsWith("}")) {
            return spel;
        }

        // 提取 #{...} 内部内容，如 "#nodeId['key']"
        String inner = spel.substring(2, spel.length() - 1).trim();

        // 内部应以 # 开头（SpEL 变量引用）
        if (!inner.startsWith("#")) {
            return spel;
        }

        // 去掉变量引用的 # 前缀
        String varExpr = inner.substring(1);

        // 解析 varName['key'] 格式
        int bracketStart = varExpr.indexOf('[');
        if (bracketStart < 0) {
            return spel;
        }

        String varName = varExpr.substring(0, bracketStart);
        String bracketPart = varExpr.substring(bracketStart);

        // 提取 ['key'] 中的 key
        if (!bracketPart.startsWith("['") || !bracketPart.endsWith("']")) {
            return spel;
        }
        String key = bracketPart.substring(2, bracketPart.length() - 2);

        if (varName.isEmpty() || key.isEmpty()) {
            return spel;
        }

        // sharedState → state.key
        if ("sharedState".equals(varName)) {
            return "state." + key;
        }

        // nodeId → nodeId.output.key（canonical 格式）
        return varName + ".output." + key;
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
     * 简单比较操作符的正则模式
     * 匹配格式: #variable op value
     * 其中 op 为 ==, !=, >=, <=, >, <
     * value 部分不允许包含 &&, ||, ? 等复合表达式字符
     */
    private static final Pattern SPEL_COMPARISON_PATTERN =
            Pattern.compile("^#(\\w+)\\s*(==|!=|>=|<=|>|<)\\s*(.+)$");

    /**
     * 字符串方法调用的正则模式
     * 匹配格式: #variable.methodName('value') 或 #variable.methodName()
     */
    private static final Pattern SPEL_METHOD_PATTERN =
            Pattern.compile("^#(\\w+)\\.(contains|startsWith|endsWith|isEmpty)\\(([^)]*)?\\)$");

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
                ConditionItem item = parseLegacySpelToItem(edge.getCondition());
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
     * <p>
     * 支持的 SpEL 模式：
     * <ul>
     *   <li>{@code #variable op value} — op 为 ==, !=, >, <, >=, <=</li>
     *   <li>{@code #variable.contains('text')} → CONTAINS</li>
     *   <li>{@code #variable.startsWith('text')} → STARTS_WITH</li>
     *   <li>{@code #variable.endsWith('text')} → ENDS_WITH</li>
     *   <li>{@code #variable.isEmpty()} → IS_EMPTY</li>
     * </ul>
     * 无法解析的复杂表达式返回 null。
     *
     * @param spelExpression SpEL 表达式字符串
     * @return 解析成功返回 ConditionItem，失败返回 null
     */
    ConditionItem parseLegacySpelToItem(String spelExpression) {
        if (spelExpression == null || spelExpression.isBlank()) {
            return null;
        }

        String trimmed = spelExpression.trim();

        // 尝试匹配比较操作符模式: #variable op value
        Matcher comparisonMatcher = SPEL_COMPARISON_PATTERN.matcher(trimmed);
        if (comparisonMatcher.matches()) {
            String variable = comparisonMatcher.group(1);
            String operator = comparisonMatcher.group(2);
            String rawValue = comparisonMatcher.group(3).trim();

            // 排除复合表达式（包含 &&, ||, ?, # 等）
            if (rawValue.contains("&&") || rawValue.contains("||")
                    || rawValue.contains("?") || rawValue.contains("#")) {
                return null;
            }

            ComparisonOperator compOp = mapSpelOperator(operator);
            if (compOp == null) {
                return null;
            }

            Object parsedValue = parseSpelLiteralValue(rawValue);
            return ConditionItem.builder()
                    .leftOperand("inputs." + variable)
                    .operator(compOp)
                    .rightOperand(parsedValue)
                    .build();
        }

        // 尝试匹配方法调用模式: #variable.method('value') 或 #variable.method()
        Matcher methodMatcher = SPEL_METHOD_PATTERN.matcher(trimmed);
        if (methodMatcher.matches()) {
            String variable = methodMatcher.group(1);
            String methodName = methodMatcher.group(2);
            String methodArg = methodMatcher.group(3);

            ComparisonOperator compOp = mapSpelMethod(methodName);
            if (compOp == null) {
                return null;
            }

            // isEmpty() 不需要右操作数
            if (compOp == ComparisonOperator.IS_EMPTY) {
                return ConditionItem.builder()
                        .leftOperand("inputs." + variable)
                        .operator(compOp)
                        .build();
            }

            // contains/startsWith/endsWith 需要解析参数
            Object parsedArg = parseSpelLiteralValue(methodArg != null ? methodArg.trim() : "");
            return ConditionItem.builder()
                    .leftOperand("inputs." + variable)
                    .operator(compOp)
                    .rightOperand(parsedArg)
                    .build();
        }

        // 无法解析
        return null;
    }

    /**
     * 将 SpEL 比较操作符映射为 ComparisonOperator
     */
    private ComparisonOperator mapSpelOperator(String spelOp) {
        return switch (spelOp) {
            case "==" -> ComparisonOperator.EQUALS;
            case "!=" -> ComparisonOperator.NOT_EQUALS;
            case ">" -> ComparisonOperator.GREATER_THAN;
            case "<" -> ComparisonOperator.LESS_THAN;
            case ">=" -> ComparisonOperator.GREATER_THAN_OR_EQUAL;
            case "<=" -> ComparisonOperator.LESS_THAN_OR_EQUAL;
            default -> null;
        };
    }

    /**
     * 将 SpEL 方法名映射为 ComparisonOperator
     */
    private ComparisonOperator mapSpelMethod(String methodName) {
        return switch (methodName) {
            case "contains" -> ComparisonOperator.CONTAINS;
            case "startsWith" -> ComparisonOperator.STARTS_WITH;
            case "endsWith" -> ComparisonOperator.ENDS_WITH;
            case "isEmpty" -> ComparisonOperator.IS_EMPTY;
            default -> null;
        };
    }

    /**
     * 解析 SpEL 字面值
     * <p>
     * 支持的格式：
     * - 单引号字符串: 'hello' → "hello"
     * - 整数: 100 → 100 (Long)
     * - 小数: 3.14 → 3.14 (Double)
     * - 布尔值: true/false → Boolean
     * - null → null
     *
     * @param rawValue 原始值字符串
     * @return 解析后的 Java 对象
     */
    private Object parseSpelLiteralValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String trimmed = rawValue.trim();

        // 单引号字符串: 'hello'
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }

        // 布尔值
        if ("true".equalsIgnoreCase(trimmed)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return Boolean.FALSE;
        }

        // null
        if ("null".equalsIgnoreCase(trimmed)) {
            return null;
        }

        // 数值: 先尝试 Long，再尝试 Double
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
            // not a long
        }
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException ignored) {
            // not a double
        }

        // 无法识别的格式，作为字符串返回
        return trimmed;
    }
}
