package com.zj.aiagent.infrastructure.interceptor;

import com.zj.aiagent.domain.workflow.entity.InterceptResult;
import com.zj.aiagent.domain.workflow.entity.NodeExecutionContext;
import com.zj.aiagent.domain.workflow.interfaces.NodeExecutionInterceptor;
import com.zj.aiagent.shared.design.workflow.StateUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 日志拦截器
 * <p>
 * 记录节点执行的开始和结束
 * </p>
 */
@Slf4j
@Component
public class LoggingInterceptor implements NodeExecutionInterceptor {

    @Override
    public InterceptResult beforeExecution(NodeExecutionContext context) {
        log.info("节点 [{}]({}) 开始执行, 类型: {}",
                context.getNodeName(),
                context.getNodeId(),
                context.getNodeType());
        return InterceptResult.proceed();
    }

    @Override
    public InterceptResult afterExecution(NodeExecutionContext context, StateUpdate update) {
        log.info("节点 [{}]({}) 执行完成, 信号: {}",
                context.getNodeName(),
                context.getNodeId(),
                update.getSignal());
        return InterceptResult.proceed();
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE; // 日志优先级最低，最后执行
    }
}
