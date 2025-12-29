package com.zj.aiagent.infrastructure.interceptor;

import com.zj.aiagent.domain.workflow.entity.InterceptResult;
import com.zj.aiagent.domain.workflow.entity.NodeExecutionContext;
import com.zj.aiagent.domain.workflow.entity.config.HumanInterventionConfig;
import com.zj.aiagent.domain.workflow.interfaces.NodeExecutionInterceptor;
import com.zj.aiagent.shared.design.workflow.ControlSignal;
import com.zj.aiagent.shared.design.workflow.StateUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 人工介入拦截器
 * <p>
 * 在节点执行前或执行后，根据配置暂停工作流，等待人工审核
 * </p>
 */
@Slf4j
@Component
public class HumanInterventionInterceptor implements NodeExecutionInterceptor {

    @Override
    public InterceptResult beforeExecution(NodeExecutionContext context) {
        HumanInterventionConfig config = context.getConfig("humanIntervention", HumanInterventionConfig.class);

        if (config != null && Boolean.TRUE.equals(config.getEnabled())
                && "BEFORE".equalsIgnoreCase(config.getTiming())) {
            log.info("节点 {} 需要人工介入（执行前）", context.getNodeId());
            return InterceptResult.pause("等待人工审核（执行前）");
        }

        return InterceptResult.proceed();
    }

    @Override
    public InterceptResult afterExecution(NodeExecutionContext context, StateUpdate update) {
        HumanInterventionConfig config = context.getConfig("humanIntervention", HumanInterventionConfig.class);

        if (config != null && Boolean.TRUE.equals(config.getEnabled())
                && "AFTER".equalsIgnoreCase(config.getTiming())
                && update.getSignal() != ControlSignal.ERROR) {
            log.info("节点 {} 需要人工介入（执行后）", context.getNodeId());
            return InterceptResult.pause("等待人工审核（执行后）");
        }

        return InterceptResult.proceed();
    }

    @Override
    public int getOrder() {
        return 100; // 人工介入优先级较高
    }
}
