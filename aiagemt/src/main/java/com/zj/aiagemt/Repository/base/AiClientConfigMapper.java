package com.zj.aiagemt.Repository.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagemt.model.entity.AiClientConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiClientConfigMapper extends BaseMapper<AiClientConfig> {
}