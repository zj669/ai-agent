package com.zj.aiagent.infrastructure.interceptor;

import com.alibaba.fastjson.JSON;
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
        // ⭐ 检查是否需要跳过人工干预（从审核恢复时，只跳过特定节点）
        String skipNodeId = context.getState().get("_SKIP_HUMAN_INTERVENTION_NODE_", String.class);
        if (skipNodeId != null && skipNodeId.equals(context.getNodeId())) {
            log.info("检测到跳过人工干预标记，本次执行跳过暂停: nodeId={}", context.getNodeId());
            // 清除标记，避免影响同一节点的下次执行（使用 remove 而不是 put null）
            context.getState().remove("_SKIP_HUMAN_INTERVENTION_NODE_");
            return InterceptResult.proceed();
        }

        HumanInterventionConfig config = context.getConfig("humanIntervention", HumanInterventionConfig.class);
        System.out.println("humanIntervention: " + JSON.toJSONString(config));

        if (config != null && Boolean.TRUE.equals(config.getEnabled())) {
            // timing 默认为 BEFORE（执行前暂停）
            String timing = config.getTiming() != null ? config.getTiming() : "BEFORE";

            if ("BEFORE".equalsIgnoreCase(timing)) {
                log.info("节点 {} 需要人工介入（执行前）", context.getNodeId());
                return InterceptResult.pause("等待人工审核（执行前）");
            }
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
