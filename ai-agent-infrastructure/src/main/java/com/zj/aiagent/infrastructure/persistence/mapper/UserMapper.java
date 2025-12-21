package com.zj.aiagent.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.persistence.entity.UserPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper - 基础设施层
 * 
 * @author zj
 * @since 2025-12-21
 */
@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
}
