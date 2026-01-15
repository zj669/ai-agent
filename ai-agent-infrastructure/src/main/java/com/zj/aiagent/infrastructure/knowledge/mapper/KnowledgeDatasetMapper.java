package com.zj.aiagent.infrastructure.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.knowledge.po.KnowledgeDatasetPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * KnowledgeDataset Mapper
 */
@Mapper
public interface KnowledgeDatasetMapper extends BaseMapper<KnowledgeDatasetPO> {
    // BaseMapper 提供了基础的 CRUD 方法
    // 如需自定义 SQL 查询,可在此添加
}
