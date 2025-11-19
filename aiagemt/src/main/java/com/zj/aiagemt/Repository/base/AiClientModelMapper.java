package com.zj.aiagemt.Repository.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagemt.model.entity.AiClientModel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiClientModelMapper extends BaseMapper<AiClientModel> {
}