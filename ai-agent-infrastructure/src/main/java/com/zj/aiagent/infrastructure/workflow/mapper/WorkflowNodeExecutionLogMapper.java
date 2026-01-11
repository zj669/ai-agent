package com.zj.aiagent.infrastructure.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zj.aiagent.infrastructure.workflow.po.WorkflowNodeExecutionLogPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowNodeExecutionLogMapper extends BaseMapper<WorkflowNodeExecutionLogPO> {
}
