package com.zj.aiagent.infrastructure.persistence.mapper;

import com.zj.aiagent.infrastructure.persistence.entity.ChatMessagePO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper {

        /**
         * 插入聊天消息
         */
        @Insert("INSERT INTO ai_chat_message (" +
                        "conversation_id, agent_id, user_id, instance_id, role, content, " +
                        "final_response, is_error, error_message, timestamp" +
                        ") VALUES (" +
                        "#{conversationId}, #{agentId}, #{userId}, #{instanceId}, #{role}, #{content}, " +
                        "#{finalResponse}, #{isError}, #{errorMessage}, #{timestamp}" +
                        ")")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(ChatMessagePO po);

        /**
         * 根据会话ID查询消息列表（按时间倒序，获取最新的 N 条）
         */
        @Select("SELECT * FROM ai_chat_message " +
                        "WHERE conversation_id = #{conversationId} " +
                        "ORDER BY timestamp DESC " +
                        "LIMIT #{limit}")
        List<ChatMessagePO> selectByConversationId(@Param("conversationId") String conversationId,
                        @Param("limit") int limit);

        /**
         * 根据会话ID删除消息
         */
        @Delete("DELETE FROM ai_chat_message WHERE conversation_id = #{conversationId}")
        int deleteByConversationId(@Param("conversationId") String conversationId);

        /**
         * 根据实例ID查询消息
         */
        @Select("SELECT * FROM ai_chat_message WHERE instance_id = #{instanceId}")
        ChatMessagePO selectByInstanceId(@Param("instanceId") Long instanceId);

        /**
         * 根据用户ID和Agent ID查询会话ID列表（按最新消息时间倒序）
         */
        @Select("SELECT conversation_id FROM ai_chat_message " +
                        "WHERE user_id = #{userId} AND agent_id = #{agentId} " +
                        "GROUP BY conversation_id " +
                        "ORDER BY MAX(timestamp) DESC")
        List<String> selectConversationIdsByUserAndAgent(@Param("userId") Long userId,
                        @Param("agentId") String agentId);
}
