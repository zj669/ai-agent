package com.zj.aiagent.domain.agent.dag.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 人工介入请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanInterventionRequest {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 审核消息
     */
    private String checkMessage;

    /**
     * 当前上下文数据(供人工查看)
     */
    private Map<String, Object> contextData;

    /**
     * 节点执行结果
     */
    private Map<String, Object> nodeResults;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 是否允许修改输出
     */
    private Boolean allowModifyOutput;

    /**
     * 是否已审核
     */
    private Boolean reviewed;

    /**
     * 是否批准
     */
    private Boolean approved;

    /**
     * 审核备注
     */
    private String comments;

    /**
     * 修改后的输出
     */
    private String modifiedOutput;

    /**
     * 审核时间
     */
    private Long reviewTime;
}
