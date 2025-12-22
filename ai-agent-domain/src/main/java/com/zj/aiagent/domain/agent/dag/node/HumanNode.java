package com.zj.aiagent.domain.agent.dag.node;

import com.zj.aiagent.domain.agent.dag.config.NodeConfig;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.context.HumanInterventionData;
import com.zj.aiagent.domain.agent.dag.context.HumanInterventionRequest;
import com.zj.aiagent.domain.agent.dag.context.HumanNodeResult;
import com.zj.aiagent.domain.agent.dag.entity.NodeType;
import com.zj.aiagent.shared.design.dag.DagNodeExecutionException;
import com.zj.aiagent.shared.design.dag.NodeExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

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
    protected NodeExecutionResult doExecute(DagExecutionContext context) throws DagNodeExecutionException {
        try {
            log.info("人工检查节点开始执行，等待人工审核");

            // 从领域对象检查是否已经有人工审核结果
            HumanInterventionData humanData = context.getHumanInterventionData();
            if (humanData.isReviewed()) {
                String humanComments = humanData.getComments("");
                log.info("人工审核已完成，审核结果: {}, 评论: {}", humanData.isApproved(), humanComments);

                if (humanData.isApproved()) {
                    return NodeExecutionResult.content(HumanNodeResult.approved(humanComments));
                } else {
                    return NodeExecutionResult.content(HumanNodeResult.rejected(humanComments));
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

            // 存储请求到领域对象
            humanData.setRequest(request);
            humanData.setPaused(nodeId);

            log.info("人工介入请求已创建，执行ID: {}, 节点ID: {}", request.getExecutionId(), nodeId);

            // 返回特殊结果表明需要人工介入
            return NodeExecutionResult.humanWait(request.getCheckMessage());

        } catch (Exception e) {
            throw new DagNodeExecutionException("人工检查节点执行失败: " + e.getMessage(), e, nodeId, false);
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.HUMAN_NODE;
    }
}
