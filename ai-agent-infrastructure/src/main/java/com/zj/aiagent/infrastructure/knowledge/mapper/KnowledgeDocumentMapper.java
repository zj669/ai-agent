package com.zj.aiagent.infrastructure.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zj.aiagent.infrastructure.knowledge.po.KnowledgeDocumentPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * KnowledgeDocument Mapper
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocumentPO> {
    // BaseMapper 提供了基础的 CRUD 方法
    // 如需自定义 SQL 查询,可在此添加
}
