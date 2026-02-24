package com.zj.aiagent.infrastructure.swarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.swarm.po.SwarmWorkspaceAgentPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SwarmAgentMapper extends BaseMapper<SwarmWorkspaceAgentPO> {
}
