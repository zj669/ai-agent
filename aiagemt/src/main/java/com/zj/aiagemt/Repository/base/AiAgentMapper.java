package com.zj.aiagemt.Repository.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagemt.model.entity.AiAgent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiAgentMapper extends BaseMapper<AiAgent> {
}