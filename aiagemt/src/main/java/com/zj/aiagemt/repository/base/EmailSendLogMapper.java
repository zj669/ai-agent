package com.zj.aiagemt.repository.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagemt.model.entity.EmailSendLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮件发送日志Mapper
 */
@Mapper
public interface EmailSendLogMapper extends BaseMapper<EmailSendLog> {
}
