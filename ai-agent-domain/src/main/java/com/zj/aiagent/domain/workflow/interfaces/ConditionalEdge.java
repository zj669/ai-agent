package com.zj.aiagent.domain.workflow.interfaces;


import com.zj.aiagent.domain.workflow.base.WorkflowState;
import com.zj.aiagent.domain.workflow.entity.RouterEntity;

import java.util.List;

public interface ConditionalEdge {

    /**
     * 根据当前状态决定下一个节点
     * 
     * @param state 当前工作流状态
     * @return 下一个节点的ID，返回 null 或 "__end__" 表示结束
     */
    List<String> evaluate(WorkflowState state, List<RouterEntity> condition);


}
