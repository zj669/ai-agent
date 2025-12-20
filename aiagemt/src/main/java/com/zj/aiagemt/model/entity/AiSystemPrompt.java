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
 * AI系统提示词配置实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_system_prompt")
public class AiSystemPrompt {

    /**
     * 主键ID
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提示词ID
     */
    @Schema(description = "提示词ID")
    private String promptId;

    /**
     * 提示词名称
     */
    @Schema(description = "提示词名称")
    private String promptName;

    /**
     * 提示词内容
     */
    @Schema(description = "提示词内容")
    private String promptContent;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String description;

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
