package com.zj.aiagent.infrastructure.parse.entity;

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
         * 【新增】模板ID - 引用 ai_node_template 表的 template_id
         * <p>
         * 如果指定了 templateId，则优先使用模板配置
         */
        private String templateId;

        /**
         * 【新增】用户自定义配置 (JSON对象)
         * <p>
         * 只包含用户可编辑的字段，会覆盖模板的默认配置
         * 例如: {"maxLoops": 3, "timeout": 30}
         */
        private String userConfig;

        /**
         * 位置信息(前端可视化)
         */
        private Position position;

        /**
         * 【保留兼容】节点配置(JSON对象)
         * <p>
         * 仅用于向后兼容，新版本应使用 templateId + userConfig
         * 
         * @deprecated 使用 templateId 和 userConfig 替代
         */
        @Deprecated
        private String config;
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
