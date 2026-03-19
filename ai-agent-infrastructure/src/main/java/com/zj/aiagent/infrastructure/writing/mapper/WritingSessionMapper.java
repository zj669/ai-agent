package com.zj.aiagent.infrastructure.writing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.writing.po.WritingSessionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WritingSessionMapper extends BaseMapper<WritingSessionPO> {
}
