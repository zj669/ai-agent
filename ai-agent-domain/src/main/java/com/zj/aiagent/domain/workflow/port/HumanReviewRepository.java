package com.zj.aiagent.domain.workflow.port;

import com.zj.aiagent.domain.workflow.entity.HumanReviewRecord;
import java.util.List;

/**
 * 人工审核仓储接口
 */
public interface HumanReviewRepository {
    /**
     * 保存审核记录
     */
    void save(HumanReviewRecord record);

    /**
     * 根据执行ID查询审核记录
     */
    List<HumanReviewRecord> findByExecutionId(String executionId);

    /**
     * 查询审核历史（简单分页：offset + limit）
     */
    List<HumanReviewRecord> findReviewHistory(Long userId, int offset, int limit);
}
