package com.zj.aiagemt.repository.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagemt.model.entity.AiClient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiClientMapper extends BaseMapper<AiClient> {
}