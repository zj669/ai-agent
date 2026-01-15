package com.zj.aiagent.infrastructure.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.workflow.po.HumanReviewPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HumanReviewMapper extends BaseMapper<HumanReviewPO> {
}
