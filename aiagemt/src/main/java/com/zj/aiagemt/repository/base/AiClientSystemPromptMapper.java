package com.zj.aiagemt.repository.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagemt.model.entity.AiClientSystemPrompt;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiClientSystemPromptMapper extends BaseMapper<AiClientSystemPrompt> {
}