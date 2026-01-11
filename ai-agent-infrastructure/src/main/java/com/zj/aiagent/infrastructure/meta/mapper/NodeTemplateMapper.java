package com.zj.aiagent.infrastructure.meta.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.meta.po.NodeTemplatePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NodeTemplateMapper extends BaseMapper<NodeTemplatePO> {
    // Basic CRUD provided by BaseMapper
}
