package com.zj.aiagent.interfaces.web.dto.response.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点执行记录DTO
 *
 * @author zj
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionRecord {

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 执行状态 (PENDING, RUNNING, COMPLETED, ERROR)
     */
    private String status;

    /**
     * 开始时间
     */
    private Long startTime;

    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 执行耗时 (毫秒)
     */
    private Long duration;

    /**
     * 节点输入
     */
    private Object input;

    /**
     * 节点输出
     */
    private Object output;

    /**
     * 错误信息 (如果执行失败)
     */
    private String error;
}
