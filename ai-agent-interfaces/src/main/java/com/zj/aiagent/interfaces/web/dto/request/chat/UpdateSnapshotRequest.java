package com.zj.aiagent.interfaces.web.dto.request.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 更新快照请求 DTO
 *
 * @author zj
 * @since 2025-12-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSnapshotRequest {

    /**
     * 要更新的节点ID
     */
    private String nodeId;

    /**
     * 更新后的状态数据
     * <p>
     * 包含需要修改的状态键值对
     */
    private Map<String, Object> stateData;
}
