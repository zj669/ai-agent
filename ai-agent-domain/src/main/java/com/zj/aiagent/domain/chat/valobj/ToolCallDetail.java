package com.zj.aiagent.domain.chat.valobj;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * 工具调用详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallDetail {
    private String toolName;
    private Map<String, Object> arguments;
    private String output;
}
