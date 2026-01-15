package com.zj.aiagent.infrastructure.workflow.stream;

import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.port.StreamPublisherFactory;
import com.zj.aiagent.domain.workflow.valobj.StreamContext;
import com.zj.aiagent.infrastructure.workflow.event.RedisSsePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Redis SSE 流式推送器工厂实现
 * 实现 StreamPublisherFactory 端口接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSseStreamPublisherFactory implements StreamPublisherFactory {

    private final RedisSsePublisher ssePublisher;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public StreamPublisher create(StreamContext context) {
        log.debug("[StreamFactory] Creating StreamPublisher for execution: {}, node: {}",
                context.getExecutionId(), context.getNodeId());
        return new RedisSseStreamPublisher(ssePublisher, objectMapper, context);
    }
}
