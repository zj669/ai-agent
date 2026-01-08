package com.zj.aiagent.infrastructure.workflow;

import com.zj.aiagent.domain.workflow.entity.RouterEntity;
import com.zj.aiagent.domain.workflow.interfaces.ConditionalEdge;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AgentConditionalEdge implements ConditionalEdge {

    @Override
    public List<String> evaluate(WorkflowState state, List<RouterEntity> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            log.warn("条件列表为空，无法进行路由选择");
            return List.of();
        }

        // 只有一个分支，直接返回
        if (conditions.size() == 1) {
            log.info("只有一个分支，直接选择: {} ({})",
                    conditions.get(0).getNodeId(), conditions.get(0).getCondition());
            return List.of(conditions.get(0).getNodeId());
        }

        // TODO: 接入 LLM 进行智能路由决策
        // 当前简化实现：默认选择第一个分支
        log.warn("⚠️ 多分支路由暂未实现 LLM 决策，默认选择第一个分支");
        log.info("可选分支:");
        for (int i = 0; i < conditions.size(); i++) {
            RouterEntity cond = conditions.get(i);
            log.info("  {}. {} → {}", i + 1, cond.getCondition(), cond.getNodeId());
        }

        String selectedNode = conditions.get(0).getNodeId();
        log.info("✅ 选择分支: {} ({})", selectedNode, conditions.get(0).getCondition());

        return List.of(selectedNode);
    }
}
