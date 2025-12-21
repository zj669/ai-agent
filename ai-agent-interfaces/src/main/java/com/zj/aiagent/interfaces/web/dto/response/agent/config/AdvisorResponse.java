package com.zj.aiagent.interfaces.web.dto.response.agent.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Advisor 响应 DTO
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Advisor 信息")
public class AdvisorResponse {

    @Schema(description = "数据库主键")
    private Long id;

    @Schema(description = "Advisor ID")
    private String advisorId;

    @Schema(description = "Advisor 名称")
    private String advisorName;

    @Schema(description = "Advisor 类型")
    private String advisorType;

    @Schema(description = "顺序号")
    private Integer orderNum;

    @Schema(description = "扩展参数配置")
    private String extParam;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
