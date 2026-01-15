package com.zj.aiagent.infrastructure.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.user.po.EmailLogPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailLogMapper extends BaseMapper<EmailLogPO>  {
}
