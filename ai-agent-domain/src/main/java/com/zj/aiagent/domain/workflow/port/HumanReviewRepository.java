package com.zj.aiagent.domain.workflow.port;

import com.zj.aiagent.domain.workflow.entity.HumanReviewRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

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
     * 查询待审核列表（如果需要从DB查的话，虽然Design建议从Redis查）
     * 这里保留查历史记录的能力
     */
    Page<HumanReviewRecord> findReviewHistory(Long userId, Pageable pageable);
}
