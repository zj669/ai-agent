package com.zj.aiagent.domain.workflow.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 执行上下文快照值对象
 * <p>
 * 表示某次工作流执行在某个节点的状态快照,用于支持暂停、恢复、查看等场景
 *
 * @author zj
 * @since 2025-12-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContextSnapshot {

    /**
     * 执行ID (conversationId)
     */
    private String executionId;

    /**
     * 最后执行的节点ID
     */
    private String lastNodeId;

    /**
     * 工作流状态数据
     * <p>
     * 包含所有状态键值对,如:
     * - 用户输入
     * - 执行历史
     * - 思考历史
     * - 循环计数
     * - 自定义变量等
     */
    private Map<String, Object> stateData;

    /**
     * 快照创建时间戳
     */
    private Long timestamp;

    /**
     * 工作流状态 (RUNNING, PAUSED, COMPLETED, ERROR)
     */
    private String status;
}
