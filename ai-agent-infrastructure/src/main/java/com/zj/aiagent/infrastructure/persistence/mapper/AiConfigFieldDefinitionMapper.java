package com.zj.aiagent.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.persistence.entity.AiConfigFieldDefinitionPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配置字段定义Mapper
 *
 * @author zj
 * @since 2025-12-23
 */
@Mapper
public interface AiConfigFieldDefinitionMapper extends BaseMapper<AiConfigFieldDefinitionPO> {
}
