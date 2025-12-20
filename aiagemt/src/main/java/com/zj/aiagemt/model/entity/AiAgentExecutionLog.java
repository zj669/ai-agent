package com.zj.aiagemt.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI智能体执行日志实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_agent_execution_log")
public class AiAgentExecutionLog {

    /**
     * 主键ID
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的运行实例ID
     */
    @Schema(description = "关联的运行实例ID")
    private Long instanceId;

    /**
     * 智能体ID
     */
    @Schema(description = "智能体ID")
    private String agentId;

    /**
     * 会话ID
     */
    @Schema(description = "会话ID")
    private String conversationId;

    /**
     * 执行的节点ID
     */
    @Schema(description = "执行的节点ID")
    private String nodeId;

    /**
     * 节点类型 (PLAN/ACT/REFLECT/ROUTER/END等)
     */
    @Schema(description = "节点类型")
    private String nodeType;

    /**
     * 节点名称
     */
    @Schema(description = "节点名称")
    private String nodeName;

    /**
     * 执行状态 (RUNNING/SUCCESS/FAILED/SKIPPED)
     */
    @Schema(description = "执行状态")
    private String executeStatus;

    /**
     * 节点输入数据(JSON格式)
     */
    @Schema(description = "节点输入数据")
    private String inputData;

    /**
     * 节点输出数据(JSON格式)
     */
    @Schema(description = "节点输出数据")
    private String outputData;

    /**
     * 错误信息(如果失败)
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 错误堆栈(如果失败)
     */
    @Schema(description = "错误堆栈")
    private String errorStack;

    /**
     * 开始执行时间
     */
    @Schema(description = "开始执行时间")
    private LocalDateTime startTime;

    /**
     * 结束执行时间
     */
    @Schema(description = "结束执行时间")
    private LocalDateTime endTime;

    /**
     * 执行耗时(毫秒)
     */
    @Schema(description = "执行耗时")
    private Long durationMs;

    /**
     * 重试次数
     */
    @Schema(description = "重试次数")
    private Integer retryCount;

    /**
     * 使用的模型信息
     */
    @Schema(description = "使用的模型信息")
    private String modelInfo;

    /**
     * Token使用情况(JSON格式)
     */
    @Schema(description = "Token使用情况")
    private String tokenUsage;

    /**
     * 其他元数据(JSON格式)
     */
    @Schema(description = "其他元数据")
    private String metadata;

    /**
     * 记录创建时间
     */
    @Schema(description = "记录创建时间")
    private LocalDateTime createTime;

}
