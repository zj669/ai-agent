package com.zj.aiagemt.repository.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagemt.model.entity.AiClientApi;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiClientApiMapper extends BaseMapper<AiClientApi> {
}