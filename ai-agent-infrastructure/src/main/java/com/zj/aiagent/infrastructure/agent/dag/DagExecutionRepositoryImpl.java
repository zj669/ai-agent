package com.zj.aiagent.infrastructure.agent.dag;

import com.zj.aiagent.domain.agent.dag.entity.DagExecutionInstance;
import com.zj.aiagent.domain.agent.dag.entity.NodeExecutionLog;
import com.zj.aiagent.domain.agent.dag.repository.IDagExecutionRepository;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentInstancePO;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentExecutionLogPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAgentInstanceMapper;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAgentExecutionLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * DAG 执行实例仓储实现
 * 负责 Entity 和 PO 之间的转换
 */
@Slf4j
@Repository
public class DagExecutionRepositoryImpl implements IDagExecutionRepository {

    @Resource
    private AiAgentInstanceMapper agentInstanceMapper;

    @Resource
    private AiAgentExecutionLogMapper executionLogMapper;

    @Override
    public DagExecutionInstance save(DagExecutionInstance instance) {
        try {
            AiAgentInstancePO po = entityToPO(instance);
            agentInstanceMapper.insert(po);
            log.info("保存 DAG 执行实例成功: id={}, conversationId={}",
                    po.getId(), po.getConversationId());
            // 将生成的ID设置回实体
            instance.setId(po.getId());
            return instance;
        } catch (Exception e) {
            log.error("保存 DAG 执行实例失败", e);
            throw new RuntimeException("保存 DAG 执行实例失败", e);
        }
    }

    @Override
    public void update(DagExecutionInstance instance) {
        try {
            AiAgentInstancePO po = entityToPO(instance);
            agentInstanceMapper.updateById(po);
            log.debug("更新 DAG 执行实例成功: id={}, status={}",
                    po.getId(), po.getStatus());
        } catch (Exception e) {
            log.error("更新 DAG 执行实例失败", e);
            throw new RuntimeException("更新 DAG 执行实例失败", e);
        }
    }

    @Override
    public DagExecutionInstance findById(Long id) {
        AiAgentInstancePO po = agentInstanceMapper.selectById(id);
        return po != null ? poToEntity(po) : null;
    }

    @Override
    public DagExecutionInstance findByConversationId(String conversationId) {
        AiAgentInstancePO po = agentInstanceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiAgentInstancePO>()
                        .eq(AiAgentInstancePO::getConversationId, conversationId)
                        .orderByDesc(AiAgentInstancePO::getCreateTime)
                        .last("LIMIT 1"));
        return po != null ? poToEntity(po) : null;
    }

    /**
     * Entity 转 PO
     */
    private AiAgentInstancePO entityToPO(DagExecutionInstance entity) {
        return AiAgentInstancePO.builder()
                .id(entity.getId())
                .agentId(entity.getAgentId())
                .conversationId(entity.getConversationId())
                .currentNodeId(entity.getCurrentNodeId())
                .status(entity.getStatus())
                .runtimeContextJson(entity.getRuntimeContextJson())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    /**
     * PO 转 Entity
     */
    private DagExecutionInstance poToEntity(AiAgentInstancePO po) {
        return DagExecutionInstance.builder()
                .id(po.getId())
                .agentId(po.getAgentId())
                .conversationId(po.getConversationId())
                .currentNodeId(po.getCurrentNodeId())
                .status(po.getStatus())
                .runtimeContextJson(po.getRuntimeContextJson())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    @Override
    public NodeExecutionLog saveNodeLog(NodeExecutionLog log1) {
        try {
            AiAgentExecutionLogPO po = nodeLogToPO(log1);
            executionLogMapper.insert(po);
            log1.setId(po.getId());
            return log1;
        } catch (Exception e) {
            log.error("保存节点执行日志失败", e);
            throw new RuntimeException("保存节点执行日志失败", e);
        }
    }

    @Override
    public void updateNodeLog(NodeExecutionLog log1) {
        try {
            AiAgentExecutionLogPO po = nodeLogToPO(log1);
            executionLogMapper.updateById(po);
        } catch (Exception e) {
            log.error("更新节点执行日志失败", e);
            throw new RuntimeException("更新节点执行日志失败", e);
        }
    }

    /**
     * NodeExecutionLog Entity 转 PO
     */
    private AiAgentExecutionLogPO nodeLogToPO(NodeExecutionLog entity) {
        return AiAgentExecutionLogPO.builder()
                .id(entity.getId())
                .instanceId(entity.getInstanceId())
                .agentId(String.valueOf(entity.getAgentId()))
                .conversationId(entity.getConversationId())
                .nodeId(entity.getNodeId())
                .nodeType(entity.getNodeType())
                .nodeName(entity.getNodeName())
                .executeStatus(entity.getExecuteStatus())
                .inputData(entity.getInputData())
                .outputData(entity.getOutputData())
                .errorMessage(entity.getErrorMessage())
                .errorStack(entity.getErrorStack())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .durationMs(entity.getDurationMs())
                .retryCount(entity.getRetryCount())
                .modelInfo(entity.getModelInfo())
                .tokenUsage(entity.getTokenUsage())
                .metadata(entity.getMetadata())
                .createTime(entity.getCreateTime())
                .build();
    }
}
