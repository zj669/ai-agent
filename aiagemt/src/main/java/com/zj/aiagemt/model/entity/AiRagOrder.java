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
 * AI知识库配置实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_rag_order")
public class AiRagOrder {

    /**
     * 主键ID
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识库ID
     */
    @Schema(description = "知识库ID")
    private String ragId;

    /**
     * 知识库名称
     */
    @Schema(description = "知识库名称")
    private String ragName;

    /**
     * 知识标签
     */
    @Schema(description = "知识标签")
    private String knowledgeTag;

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
