package com.zj.aiagent.application.agent.dto;

import com.zj.aiagent.domain.agent.entity.Agent;
import lombok.Data;

/**
 * 智能体详情响应结果
 * 用于应用层返回，避免直接暴露领域实体
 */
@Data
public class AgentDetailResult {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private String graphJson;
    private Integer version;
    private Long publishedVersionId;
    private Integer status;

    /**
     * 从领域实体转换为响应结果
     */
    public static AgentDetailResult from(Agent agent) {
        if (agent == null) {
            return null;
        }
        AgentDetailResult result = new AgentDetailResult();
        result.setId(agent.getId());
        result.setName(agent.getName());
        result.setDescription(agent.getDescription());
        result.setIcon(agent.getIcon());
        result.setGraphJson(agent.getGraphJson());
        result.setVersion(agent.getVersion());
        result.setPublishedVersionId(agent.getPublishedVersionId());
        result.setStatus(agent.getStatus() != null ? agent.getStatus().getCode() : null);
        return result;
    }
}
