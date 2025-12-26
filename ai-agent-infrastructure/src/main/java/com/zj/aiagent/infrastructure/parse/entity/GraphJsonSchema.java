package com.zj.aiagent.infrastructure.parse.entity;

import com.zj.aiagent.domain.agent.dag.entity.EdgeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphJsonSchema {

    /**
     * DAG ID
     */
    private String dagId;

    /**
     * 版本
     */
    private String version;

    /**
     * 描述
     */
    private String description;

    /**
     * 节点列表
     */
    private List<NodeDefinition> nodes;

    /**
     * 边列表
     */
    private List<EdgeDefinition> edges;

    /**
     * 起始节点ID
     */
    private String startNodeId;

    /**
     * 节点定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDefinition {
        /**
         * 节点ID
         */
        private String nodeId;

        /**
         * 节点类型 (PLAN, ACT, HUMAN, ROUTER, END)
         */
        private String nodeType;

        /**
         * 节点名称
         */
        private String nodeName;

        /**
         * 位置信息(前端可视化)
         */
        private Position position;

        /**
         * 节点配置(JSON对象)
         */
        private Object config;
    }

    /**
     * 边定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeDefinition {
        /**
         * 边ID
         */
        private String edgeId;

        /**
         * 源节点ID
         */
        private String source;

        /**
         * 目标节点ID
         */
        private String target;

        /**
         * 边标签
         */
        private String label;

        /**
         * 条件(可选)
         */
        private String condition;

        /**
         * 边类型
         * DEPENDENCY: 标准依赖边（默认）
         * LOOP_BACK: 循环边，不参与拓扑排序
         * CONDITIONAL: 条件边，由节点动态决定是否激活
         */
        @Builder.Default
        private EdgeType edgeType = EdgeType.DEPENDENCY;

        /**
         * 获取边类型，处理 null 情况
         * 
         * @return 边类型，null 时默认为 DEPENDENCY
         */
        public EdgeType getEdgeType() {
            return edgeType != null ? edgeType : EdgeType.DEPENDENCY;
        }
    }

    /**
     * 位置信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private Integer x;
        private Integer y;
    }
}
