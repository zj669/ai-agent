package com.zj.aiagent.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.persistence.entity.EmailSendLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮件发送日志Mapper
 */
@Mapper
public interface EmailSendLogMapper extends BaseMapper<EmailSendLogPO> {
}
