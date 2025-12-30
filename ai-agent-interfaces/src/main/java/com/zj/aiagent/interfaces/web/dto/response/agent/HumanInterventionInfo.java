package com.zj.aiagent.interfaces.web.dto.response.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人工介入信息DTO
 *
 * @author zj
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanInterventionInfo {

    /**
     * 暂停的节点ID
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 审核提示消息
     */
    private String checkMessage;

    /**
     * 是否允许修改输出
     */
    private Boolean allowModifyOutput;
}
