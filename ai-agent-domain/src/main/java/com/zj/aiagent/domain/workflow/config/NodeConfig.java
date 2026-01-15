package com.zj.aiagent.domain.workflow.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点配置（通用结构）
 * 使用 Map 存储配置，支持数据库驱动的动态字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeConfig {

    /**
     * 配置属性存储
     */
    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();

    /**
     * 人工审核配置
     */
    private HumanReviewConfig humanReviewConfig;

    /**
     * 重试策略
     */
    private RetryPolicy retryPolicy;

    /**
     * 超时时间（毫秒）
     */
    private Long timeoutMs;

    /**
     * 是否需要人工审核
     */
    public boolean requiresHumanReview() {
        return humanReviewConfig != null && humanReviewConfig.isEnabled();
    }

    // ========== 便捷访问方法 ==========

    /**
     * 获取字符串配置
     */
    public String getString(String key) {
        Object value = properties.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 获取字符串配置，带默认值
     */
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取整数配置
     */
    public Integer getInteger(String key) {
        Object value = properties.get(key);
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

    /**
     * 获取整数配置，带默认值
     */
    public int getInteger(String key, int defaultValue) {
        Integer value = getInteger(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取长整数配置
     */
    public Long getLong(String key) {
        Object value = properties.get(key);
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

    /**
     * 获取浮点数配置
     */
    public Double getDouble(String key) {
        Object value = properties.get(key);
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

    /**
     * 获取布尔配置
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * 获取列表配置
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        Object value = properties.get(key);
        if (value instanceof List) {
            return (List<T>) value;
        }
        return null;
    }

    /**
     * 获取 Map 配置
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String key) {
        Object value = properties.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    /**
     * 设置配置项
     */
    public void set(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * 检查是否包含配置项
     */
    public boolean has(String key) {
        return properties.containsKey(key) && properties.get(key) != null;
    }
}
