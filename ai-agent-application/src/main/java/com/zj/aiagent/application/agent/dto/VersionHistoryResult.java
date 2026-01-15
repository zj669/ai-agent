package com.zj.aiagent.application.agent.dto;

import com.zj.aiagent.domain.agent.entity.AgentVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 版本历史查询结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionHistoryResult {

    /**
     * 版本列表
     */
    private List<VersionItem> versions;

    /**
     * 从领域实体转换
     */
    public static VersionHistoryResult from(List<AgentVersion> versions) {
        List<VersionItem> items = versions.stream()
                .map(VersionItem::from)
                .collect(Collectors.toList());
        return VersionHistoryResult.builder()
                .versions(items)
                .build();
    }

    /**
     * 单个版本项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionItem {
        private Long id;
        private Long agentId;
        private Integer version;
        private String description;
        private LocalDateTime createTime;

        public static VersionItem from(AgentVersion v) {
            return VersionItem.builder()
                    .id(v.getId())
                    .agentId(v.getAgentId())
                    .version(v.getVersion())
                    .description(v.getDescription())
                    .createTime(v.getCreateTime())
                    .build();
        }
    }
}
