package com.zj.aiagent.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentTaskSchedulePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiAgentTaskScheduleMapper extends BaseMapper<AiAgentTaskSchedulePO> {
}