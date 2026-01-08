package com.zj.aiagent.infrastructure.persistence.repository;

import com.zj.aiagent.infrastructure.persistence.entity.AiAgentInstancePO;

/**
 * AI智能体运行实例仓储接口
 */
public interface IAiAgentInstanceRepository {

    /**
     * 根据会话ID查询最新的实例
     * 
     * @param conversationId 会话ID
     * @return 实例PO，不存在返回null
     */
    AiAgentInstancePO findByConversationId(String conversationId);

    /**
     * 保存或更新实例
     * 如果实例已存在（有ID），则更新；否则插入新记录
     * 
     * @param instance 实例PO
     * @return 保存后的实例（包含生成的ID）
     */
    AiAgentInstancePO saveOrUpdate(AiAgentInstancePO instance);

    /**
     * 清空实例的检查点数据
     * 
     * @param conversationId 会话ID
     */
    void clearCheckpointData(String conversationId);
}
