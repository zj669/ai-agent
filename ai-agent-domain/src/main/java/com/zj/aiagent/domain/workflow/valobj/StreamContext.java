package com.zj.aiagent.domain.workflow.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式上下文值对象
 * 纯数据载体，不持有任何接口引用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamContext {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 父节点ID（并行组ID，支持树形结构）
     */
    private String parentId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 节点名称
     */
    private String nodeName;
}
