package com.zj.aiagent.infrastructure.workflow.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.entity.WorkflowNodeExecutionLog;
import com.zj.aiagent.domain.workflow.port.WorkflowNodeExecutionLogRepository;
import com.zj.aiagent.infrastructure.workflow.mapper.WorkflowNodeExecutionLogMapper;
import com.zj.aiagent.infrastructure.workflow.po.WorkflowNodeExecutionLogPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WorkflowNodeExecutionLogRepositoryImpl implements WorkflowNodeExecutionLogRepository {

    private final WorkflowNodeExecutionLogMapper logMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void save(WorkflowNodeExecutionLog logDomain) {
        WorkflowNodeExecutionLogPO po = toPO(logDomain);
        logMapper.insert(po);
        logDomain.setId(po.getId());
    }

    @Override
    public List<WorkflowNodeExecutionLog> findByExecutionId(String executionId) {
        LambdaQueryWrapper<WorkflowNodeExecutionLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowNodeExecutionLogPO::getExecutionId, executionId)
                .orderByAsc(WorkflowNodeExecutionLogPO::getStartTime);
        return logMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public WorkflowNodeExecutionLog findByExecutionIdAndNodeId(String executionId, String nodeId) {
        LambdaQueryWrapper<WorkflowNodeExecutionLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowNodeExecutionLogPO::getExecutionId, executionId)
                .eq(WorkflowNodeExecutionLogPO::getNodeId, nodeId);
        return toDomain(logMapper.selectOne(wrapper));
    }

    // --- Converters ---

    private WorkflowNodeExecutionLogPO toPO(WorkflowNodeExecutionLog domain) {
        if (domain == null)
            return null;
        WorkflowNodeExecutionLogPO po = new WorkflowNodeExecutionLogPO();
        po.setId(domain.getId());
        po.setExecutionId(domain.getExecutionId());
        po.setNodeId(domain.getNodeId());
        po.setNodeName(domain.getNodeName());
        po.setNodeType(domain.getNodeType());
        po.setRenderMode(domain.getRenderMode());
        po.setStatus(domain.getStatus());
        po.setErrorMessage(domain.getErrorMessage());
        po.setStartTime(domain.getStartTime());
        po.setEndTime(domain.getEndTime());

        po.setInputs(objectMapper.valueToTree(domain.getInputs()));
        po.setOutputs(objectMapper.valueToTree(domain.getOutputs()));

        return po;
    }

    private WorkflowNodeExecutionLog toDomain(WorkflowNodeExecutionLogPO po) {
        if (po == null)
            return null;
        return WorkflowNodeExecutionLog.builder()
                .id(po.getId())
                .executionId(po.getExecutionId())
                .nodeId(po.getNodeId())
                .nodeName(po.getNodeName())
                .nodeType(po.getNodeType())
                .renderMode(po.getRenderMode())
                .status(po.getStatus())
                .errorMessage(po.getErrorMessage())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .inputs(objectMapper.convertValue(po.getInputs(), Map.class))
                .outputs(objectMapper.convertValue(po.getOutputs(), Map.class))
                .build();
    }
}
