package com.zj.aiagent.domain.agent.dag.node;

import com.zj.aiagent.domain.agent.dag.config.NodeConfig;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.context.HumanInterventionRequest;
import com.zj.aiagent.domain.agent.dag.context.HumanNodeResult;

import com.zj.aiagent.shared.design.dag.DagNodeExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import com.zj.aiagent.domain.agent.dag.entity.NodeType;
/**
 * 人工检查节点 - 支持人工介入和修改context
 * 暂停DAG执行,等待人工审核,支持修改context,人工决策继续或终止
 */
@Slf4j
public class HumanNode extends AbstractConfigurableNode {

    public HumanNode(String nodeId, String nodeName, NodeConfig config, ApplicationContext applicationContext) {
        super(nodeId, nodeName, config, applicationContext);
    }

    @Override
    protected String doExecute(DagExecutionContext context) throws DagNodeExecutionException {
        try {
            log.info("人工检查节点开始执行，等待人工审核");

            // 检查是否已经有人工审核结果(恢复执行的情况)
            Boolean humanApproved = context.getValue("human_approved");
            if (humanApproved != null) {
                String humanComments = context.getValue("human_comments", "");
                log.info("人工审核已完成，审核结果: {}, 评论: {}", humanApproved, humanComments);

                if (Boolean.TRUE.equals(humanApproved)) {
                    return HumanNodeResult.approved(humanComments);
                } else {
                    return HumanNodeResult.rejected(humanComments);
                }
            }

            // 创建人工介入请求
            String checkMessage = (String) config.getCustomConfig().getOrDefault("checkMessage", "请审核任务执行结果");

            HumanInterventionRequest request = HumanInterventionRequest.builder()
                    .executionId(context.getExecutionId())
                    .nodeId(nodeId)
                    .conversationId(context.getConversationId())
                    .checkMessage(checkMessage)
                    .nodeResults(context.getAllNodeResults())
                    .createTime(System.currentTimeMillis())
                    .build();

            // 存储请求到context供后续API查询
            context.setValue("human_intervention_request", request);
            context.setValue("paused_at", System.currentTimeMillis());
            context.setValue("paused_node_id", nodeId);

            log.info("人工介入请求已创建，执行ID: {}, 节点ID: {}", request.getExecutionId(), nodeId);

            // 返回特殊结果表明需要人工介入
            return HumanNodeResult.waitingForHuman(request);

        } catch (Exception e) {
            throw new DagNodeExecutionException("人工检查节点执行失败: " + e.getMessage(), e, nodeId, false);
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.HUMAN_NODE;
    }
}
