package com.zj.aiagent.infrastructure.persistence.mapper;

import com.zj.aiagent.infrastructure.persistence.entity.AgentExecutionLogPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Agent 执行日志 Mapper
 */
@Mapper
public interface AgentExecutionLogMapper {

    /**
     * 插入执行日志
     */
    @Insert("INSERT INTO ai_agent_execution_log (" +
            "instance_id, agent_id, conversation_id, node_id, node_type, node_name, " +
            "execute_status, input_data, output_data, error_message, error_stack, " +
            "start_time, end_time, duration_ms, retry_count, model_info, token_usage, metadata" +
            ") VALUES (" +
            "#{instanceId}, #{agentId}, #{conversationId}, #{nodeId}, #{nodeType}, #{nodeName}, " +
            "#{executeStatus}, #{inputData}, #{outputData}, #{errorMessage}, #{errorStack}, " +
            "#{startTime}, #{endTime}, #{durationMs}, #{retryCount}, #{modelInfo}, #{tokenUsage}, #{metadata}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentExecutionLogPO po);

    /**
     * 根据实例ID列表批量查询执行日志（用于关联查询）
     */
    @Select("<script>" +
            "SELECT * FROM ai_agent_execution_log WHERE instance_id IN " +
            "<foreach collection='instanceIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " ORDER BY start_time ASC" +
            "</script>")
    List<AgentExecutionLogPO> selectByInstanceIds(@Param("instanceIds") List<Long> instanceIds);

    /**
     * 根据实例ID查询执行日志
     */
    @Select("SELECT * FROM ai_agent_execution_log " +
            "WHERE instance_id = #{instanceId} " +
            "ORDER BY start_time ASC")
    List<AgentExecutionLogPO> selectByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * 根据会话ID查询执行日志
     */
    @Select("SELECT * FROM ai_agent_execution_log " +
            "WHERE conversation_id = #{conversationId} " +
            "ORDER BY start_time DESC " +
            "LIMIT #{limit}")
    List<AgentExecutionLogPO> selectByConversationId(@Param("conversationId") String conversationId,
            @Param("limit") int limit);

    /**
     * 根据会话ID删除执行日志
     */
    @Delete("DELETE FROM ai_agent_execution_log WHERE conversation_id = #{conversationId}")
    int deleteByConversationId(@Param("conversationId") String conversationId);
}
