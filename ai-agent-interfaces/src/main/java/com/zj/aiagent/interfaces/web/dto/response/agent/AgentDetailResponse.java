package com.zj.aiagent.interfaces.web.dto.response.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent详情响应
 *
 * @author zj
 * @since 2025-12-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent详情响应")
public class AgentDetailResponse {

    @Schema(description = "Agent ID", example = "agent-123")
    private String agentId;

    @Schema(description = "Agent名称", example = "翻译助手")
    private String agentName;

    @Schema(description = "描述", example = "智能翻译助手")
    private String description;

    @Schema(description = "状态（0:草稿, 1:已发布, 2:已停用）", example = "0")
    private Integer status;

    @Schema(description = "状态描述", example = "草稿")
    private String statusDesc;

    @Schema(description = "DAG配置JSON")
    private String graphJson;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
