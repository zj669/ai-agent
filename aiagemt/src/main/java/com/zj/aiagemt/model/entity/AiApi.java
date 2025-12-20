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
 * AI API配置实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_api")
public class AiApi {

    /**
     * 主键ID
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * API ID
     */
    @Schema(description = "API ID")
    private String apiId;

    /**
     * 基础URL
     */
    @Schema(description = "基础URL")
    private String baseUrl;

    /**
     * API密钥
     */
    @Schema(description = "API密钥")
    private String apiKey;

    /**
     * 对话补全路径
     */
    @Schema(description = "对话补全路径")
    private String completionsPath;

    /**
     * 嵌入向量路径
     */
    @Schema(description = "嵌入向量路径")
    private String embeddingsPath;

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
