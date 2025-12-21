package com.zj.aiagent.interfaces.web.dto.response.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 响应 DTO
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent 信息")
public class AgentResponse {

    @Schema(description = "主键ID")
    private Long id;


    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态 (0:草稿, 1:已发布, 2:已停用)")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
