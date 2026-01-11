package com.zj.aiagent.infrastructure.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.agent.po.AgentVersionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentVersionMapper extends BaseMapper<AgentVersionPO> {

    @Select("SELECT COALESCE(MAX(version), 0) FROM agent_version WHERE agent_id = #{agentId}")
    Integer selectMaxVersion(@Param("agentId") Long agentId);

    @Select("SELECT * FROM agent_version WHERE agent_id = #{agentId} ORDER BY version DESC")
    List<AgentVersionPO> selectHistory(@Param("agentId") Long agentId);
}
