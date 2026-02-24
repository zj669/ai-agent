package com.zj.aiagent.infrastructure.swarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.swarm.po.SwarmGroupMemberPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SwarmGroupMemberMapper extends BaseMapper<SwarmGroupMemberPO> {

    @Select("SELECT agent_id FROM swarm_group_member WHERE group_id = #{groupId}")
    List<Long> selectMemberIds(@Param("groupId") Long groupId);

    @Update("UPDATE swarm_group_member SET last_read_message_id = #{messageId} WHERE group_id = #{groupId} AND agent_id = #{agentId}")
    void updateLastReadMessageId(@Param("groupId") Long groupId, @Param("agentId") Long agentId, @Param("messageId") Long messageId);

    @Select("SELECT COALESCE(last_read_message_id, 0) FROM swarm_group_member WHERE group_id = #{groupId} AND agent_id = #{agentId}")
    Long selectLastReadMessageId(@Param("groupId") Long groupId, @Param("agentId") Long agentId);
}
