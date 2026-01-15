package com.zj.aiagent.domain.workflow.port;

import com.zj.aiagent.domain.workflow.valobj.StreamContext;

/**
 * 流式推送器工厂端口接口
 * 用于创建 StreamPublisher 实例
 * 
 * 实现类位于 Infrastructure 层
 */
public interface StreamPublisherFactory {

    /**
     * 根据流式上下文创建推送器
     * 
     * @param context 流式上下文（包含执行ID、节点ID等信息）
     * @return StreamPublisher 实例
     */
    StreamPublisher create(StreamContext context);
}
