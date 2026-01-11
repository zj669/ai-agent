package com.zj.aiagent.domain.workflow.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * HTTP 节点配置
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HttpNodeConfig extends NodeConfig {

    /**
     * 请求 URL（支持 SpEL）
     */
    private String url;

    /**
     * HTTP 方法
     */
    private String method;

    /**
     * 请求头（支持 SpEL）
     */
    private Map<String, String> headers;

    /**
     * 请求体模板（支持 SpEL）
     */
    private String bodyTemplate;

    /**
     * 请求体类型
     */
    private String contentType;

    /**
     * 响应结果的提取路径（JSONPath）
     */
    private String responseExtractor;

    /**
     * 连接超时（毫秒）
     */
    private Long connectTimeoutMs;

    /**
     * 读取超时（毫秒）
     */
    private Long readTimeoutMs;
}
