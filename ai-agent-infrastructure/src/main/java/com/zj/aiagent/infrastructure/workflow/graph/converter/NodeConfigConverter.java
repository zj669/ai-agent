package com.zj.aiagent.infrastructure.workflow.graph.converter;

import com.zj.aiagent.domain.workflow.config.*;
import com.zj.aiagent.domain.workflow.valobj.Branch;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NodeConfig 多态转换器
 * 根据节点类型将 userConfig Map 转换为对应的 NodeConfig 子类
 * 
 * 支持解析：
 * 1. 节点特定配置（LLM、HTTP、Condition 等）
 * 2. 通用配置（重试策略、人工审核）
 */
@Component
public class NodeConfigConverter {

    /**
     * 转换 userConfig 为对应的 NodeConfig 子类
     *
     * @param nodeType   节点类型
     * @param userConfig 用户配置 Map
     * @return NodeConfig 子类实例
     */
    public NodeConfig convert(NodeType nodeType, Map<String, Object> userConfig) {
        if (userConfig == null) {
            return null;
        }

        // 1. 根据节点类型创建特定配置
        NodeConfig config = createTypeSpecificConfig(nodeType, userConfig);

        if (config == null) {
            return null;
        }

        // 2. 解析通用配置（重试策略、人工审核、超时）
        applyCommonConfig(config, userConfig);

        return config;
    }

    /**
     * 创建节点类型特定的配置
     */
    private NodeConfig createTypeSpecificConfig(NodeType nodeType, Map<String, Object> userConfig) {
        return switch (nodeType) {
            case LLM -> convertLlmConfig(userConfig);
            case HTTP -> convertHttpConfig(userConfig);
            case CONDITION -> convertConditionConfig(userConfig);
            default -> createDefaultConfig(userConfig);
        };
    }

    /**
     * 为没有特定配置的节点创建默认配置
     */
    private NodeConfig createDefaultConfig(Map<String, Object> userConfig) {
        // 创建一个简单的配置容器，只包含通用配置
        return LlmNodeConfig.builder().build(); // 使用 LlmNodeConfig 作为容器
    }

    /**
     * 应用通用配置到 NodeConfig
     */
    private void applyCommonConfig(NodeConfig config, Map<String, Object> userConfig) {
        // 1. 解析重试策略
        Object retryObj = userConfig.get("retryPolicy");
        if (retryObj instanceof Map) {
            config.setRetryPolicy(convertRetryPolicy((Map<?, ?>) retryObj));
        }

        // 2. 解析人工审核配置
        Object humanReviewObj = userConfig.get("humanReviewConfig");
        if (humanReviewObj == null) {
            humanReviewObj = userConfig.get("humanReview");
        }
        if (humanReviewObj instanceof Map) {
            config.setHumanReviewConfig(convertHumanReviewConfig((Map<?, ?>) humanReviewObj));
        }

        // 3. 解析超时配置（如果在 userConfig 顶层）
        Long timeout = getLong(userConfig, "timeout");
        if (timeout == null) {
            timeout = getLong(userConfig, "timeoutMs");
        }
        if (timeout != null && config.getTimeoutMs() == null) {
            config.setTimeoutMs(timeout);
        }
    }

    /**
     * 转换重试策略
     */
    private RetryPolicy convertRetryPolicy(Map<?, ?> retryMap) {
        return RetryPolicy.builder()
                .maxRetries(getIntegerFromMap(retryMap, "maxRetries", 3))
                .retryDelayMs(getLongFromMap(retryMap, "retryDelayMs", 1000L))
                .exponentialBackoff(getBooleanFromMap(retryMap, "exponentialBackoff", true))
                .backoffMultiplier(getDoubleFromMap(retryMap, "backoffMultiplier", 2.0))
                .maxRetryDelayMs(getLongFromMap(retryMap, "maxRetryDelayMs", 30000L))
                .build();
    }

    /**
     * 转换人工审核配置
     */
    private HumanReviewConfig convertHumanReviewConfig(Map<?, ?> reviewMap) {
        HumanReviewConfig.HumanReviewConfigBuilder builder = HumanReviewConfig.builder()
                .enabled(getBooleanFromMap(reviewMap, "enabled", false))
                .prompt((String) reviewMap.get("prompt"));

        // 解析可编辑字段
        Object editableFieldsObj = reviewMap.get("editableFields");
        if (editableFieldsObj instanceof List<?> fieldsList) {
            String[] fields = fieldsList.stream()
                    .filter(f -> f instanceof String)
                    .map(Object::toString)
                    .toArray(String[]::new);
            builder.editableFields(fields);
        } else if (editableFieldsObj instanceof String[]) {
            builder.editableFields((String[]) editableFieldsObj);
        }

        return builder.build();
    }

    private LlmNodeConfig convertLlmConfig(Map<String, Object> config) {
        return LlmNodeConfig.builder()
                .model(getString(config, "model"))
                .systemPrompt(getString(config, "systemPrompt"))
                .promptTemplate(getString(config, "userPromptTemplate"))
                .temperature(getDouble(config, "temperature"))
                .maxTokens(getInteger(config, "maxTokens"))
                .stream(getBoolean(config, "stream", false))
                .timeoutMs(getLong(config, "timeout"))
                // 新增：解析记忆和环境感知配置
                .includeExecutionLog(getBoolean(config, "includeExecutionLog", true))
                .includeChatHistory(getBoolean(config, "includeChatHistory", true))
                .maxHistoryRounds(getIntegerOrDefault(config, "maxHistoryRounds", 10))
                .contextRefNodes(getStringList(config, "contextRefNodes"))
                .build();
    }

    private HttpNodeConfig convertHttpConfig(Map<String, Object> config) {
        return HttpNodeConfig.builder()
                .url(getString(config, "url"))
                .method(getString(config, "method"))
                .headers(getStringMap(config, "headers"))
                .bodyTemplate(getString(config, "bodyTemplate"))
                .contentType(getString(config, "contentType"))
                .responseExtractor(getString(config, "responseExtractor"))
                .connectTimeoutMs(getLong(config, "connectTimeout"))
                .readTimeoutMs(getLong(config, "readTimeout"))
                .build();
    }

    private ConditionNodeConfig convertConditionConfig(Map<String, Object> config) {
        ConditionNodeConfig.ConditionNodeConfigBuilder<?, ?> builder = ConditionNodeConfig.builder()
                .defaultBranchId(getString(config, "defaultBranchId"));

        // 解析路由策略
        String strategy = getString(config, "routingStrategy");
        if (strategy != null) {
            builder.routingStrategy(ConditionNodeConfig.RoutingStrategy.valueOf(strategy.toUpperCase()));
        }

        // 解析分支
        Object branchesObj = config.get("branches");
        if (branchesObj instanceof List<?> branchesList) {
            List<Branch> branches = branchesList.stream()
                    .filter(b -> b instanceof Map)
                    .map(b -> convertBranch((Map<?, ?>) b))
                    .collect(Collectors.toList());
            builder.branches(branches);
        }

        return builder.build();
    }

    private Branch convertBranch(Map<?, ?> branchMap) {
        return Branch.builder()
                .branchId((String) branchMap.get("branchId"))
                .label((String) branchMap.get("branchName"))
                .description((String) branchMap.get("description"))
                .condition((String) branchMap.get("expression"))
                .build();
    }

    // --- 辅助方法 ---

    private String getString(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private int getIntegerOrDefault(Map<String, Object> config, String key, int defaultValue) {
        Integer value = getInteger(config, key);
        return value != null ? value : defaultValue;
    }

    private Long getLong(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Double getDouble(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getStringMap(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Map) {
            return ((Map<?, ?>) value).entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> e.getValue() != null ? e.getValue().toString() : ""));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof String)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return null;
    }

    // --- 通用 Map 辅助方法（用于 Map<?, ?> 类型）---

    private int getIntegerFromMap(Map<?, ?> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long getLongFromMap(Map<?, ?> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private double getDoubleFromMap(Map<?, ?> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getBooleanFromMap(Map<?, ?> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
}
