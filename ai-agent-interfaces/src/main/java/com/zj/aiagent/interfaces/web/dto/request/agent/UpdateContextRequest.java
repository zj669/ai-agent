package com.zj.aiagent.interfaces.web.dto.request.agent;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 更新上下文请求DTO
 *
 * @author zj
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContextRequest {

    /**
     * 需要修改的字段
     */
    @NotNull(message = "modifications不能为空")
    private Map<String, Object> modifications;
}
