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
 * AI顾问实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_advisor")
public class AiAdvisor {

    /**
     * 主键ID
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 顾问ID
     */
    @Schema(description = "顾问ID")
    private String advisorId;

    /**
     * 顾问名称
     */
    @Schema(description = "顾问名称")
    private String advisorName;

    /**
     * 顾问类型(PromptChatMemory/RagAnswer/SimpleLoggerAdvisor等)
     */
    @Schema(description = "顾问类型")
    private String advisorType;

    /**
     * 顺序号
     */
    @Schema(description = "顺序号")
    private Integer orderNum;

    /**
     * 扩展参数配置，json 记录
     */
    @Schema(description = "扩展参数配置")
    private String extParam;

    /**
     * 状态(0:禁用,1:启用)
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
