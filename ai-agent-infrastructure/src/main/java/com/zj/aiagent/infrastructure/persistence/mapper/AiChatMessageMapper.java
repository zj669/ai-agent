package com.zj.aiagent.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.persistence.entity.AiChatMessagePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessagePO> {
}
