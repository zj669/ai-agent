package com.zj.aiagent.infrastructure.llm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.llm.po.LlmProviderConfigPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmProviderConfigMapper extends BaseMapper<LlmProviderConfigPO> {
}
