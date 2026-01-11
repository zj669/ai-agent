package com.zj.aiagent.infrastructure.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.agent.po.AgentPO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentMapper extends BaseMapper<AgentPO> {

    // --- AgentVersion Methods can be here or in separate Mapper.
    // Usually separate mapper is cleaner, but for simplicity I can put insert logic
    // here
    // or just assume AgentVersionMapper exists?
    // Task list said "AgentMapper.java" and "Implement ... for agent_info and
    // agent_version tables".
    // I should create AgentVersionMapper as well for standard practice,
    // OR just put custom SQL here. But BaseMapper is bound to one Entity.
    // I will Create AgentVersionMapper separately for correctness.

    // Custom query to select specific fields (Optimized 列表查询)
    // Excluding graph_json
    @Select("SELECT id, user_id, name, description, icon, status, published_version_id, version, deleted, create_time, update_time FROM agent_info WHERE user_id = #{userId} AND deleted = 0")
    List<AgentPO> selectSummaryByUserId(@Param("userId") Long userId);
}
