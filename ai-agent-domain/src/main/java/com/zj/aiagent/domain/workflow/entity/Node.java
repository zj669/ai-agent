package com.zj.aiagent.domain.workflow.entity;

import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

/**
 * 工作流节点实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Node {

    /**
     * 节点ID（图内唯一）
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点类型
     */
    private NodeType type;

    /**
     * 节点配置
     */
    private NodeConfig config;

    /**
     * 输入参数映射（支持 SpEL 表达式）
     * key: 参数名, value: 表达式或静态值
     */
    private Map<String, Object> inputs;

    /**
     * 输出参数映射
     * key: 上下文中的 key, value: 从结果中提取的路径
     */
    private Map<String, String> outputs;

    /**
     * 依赖的上游节点ID
     */
    private Set<String> dependencies;

    /**
     * 下游节点ID
     */
    private Set<String> successors;

    /**
     * 位置信息（用于前端渲染）
     */
    private Position position;

    /**
     * 是否需要人工审核
     */
    public boolean requiresHumanReview() {
        return config != null && config.requiresHumanReview();
    }

    /**
     * 是否为起始节点
     */
    public boolean isStartNode() {
        return type == NodeType.START;
    }

    /**
     * 是否为结束节点
     */
    public boolean isEndNode() {
        return type == NodeType.END;
    }

    /**
     * 是否为条件节点
     */
    public boolean isConditionNode() {
        return type == NodeType.CONDITION;
    }

    /**
     * 位置信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private double x;
        private double y;
    }
}
