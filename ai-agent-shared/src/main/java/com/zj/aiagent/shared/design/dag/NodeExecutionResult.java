package com.zj.aiagent.shared.design.dag;

/**
 * 统一的节点执行结果
 * 用于包装所有节点类型的执行结果，包括普通节点和路由节点
 */
public class NodeExecutionResult {

    /**
     * 结果类型
     */
    public enum ResultType {
        /** 普通内容输出 */
        NORMAL,
        /** 路由决策 */
        ROUTING,
        /** 等待人工介入 */
        HUMAN_WAIT
    }

    private final String content;
    private final NodeRouteDecision routeDecision;
    private final ResultType type;

    private NodeExecutionResult(String content, NodeRouteDecision routeDecision, ResultType type) {
        this.content = content;
        this.routeDecision = routeDecision;
        this.type = type;
    }

    /**
     * 创建普通内容结果
     */
    public static NodeExecutionResult content(String content) {
        return new NodeExecutionResult(content, null, ResultType.NORMAL);
    }

    /**
     * 创建普通内容结果（支持 Object 类型）
     */
    public static NodeExecutionResult content(Object content) {
        String contentStr = content != null ? content.toString() : null;
        return new NodeExecutionResult(contentStr, null, ResultType.NORMAL);
    }

    /**
     * 创建路由决策结果
     */
    public static NodeExecutionResult routing(NodeRouteDecision decision) {
        return new NodeExecutionResult(null, decision, ResultType.ROUTING);
    }

    /**
     * 创建路由决策结果（带内容描述）
     */
    public static NodeExecutionResult routing(NodeRouteDecision decision, String description) {
        return new NodeExecutionResult(description, decision, ResultType.ROUTING);
    }

    /**
     * 创建人工等待结果
     */
    public static NodeExecutionResult humanWait(String message) {
        return new NodeExecutionResult("WAITING_FOR_HUMAN:" + message, null, ResultType.HUMAN_WAIT);
    }

    /**
     * 创建错误结果
     */
    public static NodeExecutionResult error(String errorMessage) {
        return new NodeExecutionResult("ERROR:" + errorMessage, null, ResultType.NORMAL);
    }

    /**
     * 是否为路由决策结果
     */
    public boolean isRoutingDecision() {
        return type == ResultType.ROUTING && routeDecision != null;
    }

    /**
     * 是否为人工等待结果
     */
    public boolean isHumanWait() {
        return type == ResultType.HUMAN_WAIT;
    }

    /**
     * 获取内容（普通节点的输出或路由描述）
     */
    public String getContent() {
        return content;
    }

    /**
     * 获取路由决策
     */
    public NodeRouteDecision getRouteDecision() {
        return routeDecision;
    }

    /**
     * 获取结果类型
     */
    public ResultType getType() {
        return type;
    }

    @Override
    public String toString() {
        if (isRoutingDecision()) {
            return "NodeExecutionResult{type=ROUTING, decision=" + routeDecision.getNextNodeIds() + "}";
        } else if (isHumanWait()) {
            return "NodeExecutionResult{type=HUMAN_WAIT, content=" + content + "}";
        } else {
            return "NodeExecutionResult{type=NORMAL, content=" +
                    (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + "}";
        }
    }
}
