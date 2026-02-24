package com.zj.aiagent.infrastructure.swarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.swarm.po.SwarmMessagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SwarmMessageMapper extends BaseMapper<SwarmMessagePO> {

    @Select("SELECT COUNT(*) FROM swarm_message m " +
            "JOIN swarm_group_member gm1 ON m.group_id = gm1.group_id AND gm1.agent_id = #{agentId1} " +
            "JOIN swarm_group_member gm2 ON m.group_id = gm2.group_id AND gm2.agent_id = #{agentId2} " +
            "WHERE m.workspace_id = #{workspaceId}")
    int countMessagesBetween(@Param("workspaceId") Long workspaceId,
                             @Param("agentId1") Long agentId1,
                             @Param("agentId2") Long agentId2);
}
