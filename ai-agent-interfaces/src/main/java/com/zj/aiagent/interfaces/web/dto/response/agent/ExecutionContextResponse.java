package com.zj.aiagent.interfaces.web.dto.response.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 执行上下文响应DTO
 *
 * @author zj
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContextResponse {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 最后执行的节点ID
     */
    private String lastNodeId;

    /**
     * 执行状态 (RUNNING, PAUSED, COMPLETED, ERROR)
     */
    private String status;

    /**
     * 快照时间戳
     */
    private Long timestamp;

    /**
     * 工作流状态数据
     * <p>
     * 包含所有状态键值对，如用户输入、执行历史、思考历史等
     */
    private Map<String, Object> stateData;

    /**
     * 人工介入信息
     * <p>
     * 仅当 status=PAUSED 时有值，提供人工介入相关的详细信息
     */
    private HumanInterventionInfo humanIntervention;

    /**
     * 节点执行历史
     * <p>
     * 标准化的节点执行记录列表，包含每个节点的执行状态、耗时等信息
     */
    private List<NodeExecutionRecord> executionHistory;

    /**
     * 可编辑字段元数据
     * <p>
     * 后端驱动的字段定义，告诉前端哪些 stateData 字段可以编辑以及如何展示
     */
    private List<EditableFieldMeta> editableFields;
}
