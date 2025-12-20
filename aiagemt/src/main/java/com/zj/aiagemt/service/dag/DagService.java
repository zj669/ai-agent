package com.zj.aiagemt.service.dag;

import com.zj.aiagemt.service.dag.context.DagExecutionContext;
import com.zj.aiagemt.service.dag.loader.DagLoaderService;
import com.zj.aiagemt.service.dag.model.DagGraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * DAG服务 - 提供DAG加载和执行的统一入口
 * (示例代码，展示如何使用DagLoaderService)
 */
@Slf4j
@Service
public class DagService {

    private final DagLoaderService dagLoaderService;

    public DagService(DagLoaderService dagLoaderService) {
        this.dagLoaderService = dagLoaderService;
    }

    /**
     * 加载DAG示例
     */
    public DagGraph loadDag(Long agentId, String version) {
        log.info("加载DAG: agentId={}, version={}", agentId, version);

        // 使用DagLoaderService加载DAG
        DagGraph dagGraph = dagLoaderService.loadDagByAgentIdAndVersion(agentId, version);

        log.info("DAG加载成功: dagId={}, 节点数={}",
                dagGraph.getDagId(), dagGraph.getNodes().size());

        return dagGraph;
    }

    /**
     * 执行DAG示例(简单版本，完整版本需要DAG执行引擎)
     */
    public String executeDagSimple(Long versionId, String conversationId, String userInput) {
        try {
            // 1. 加载DAG
            DagGraph dagGraph = dagLoaderService.loadDagByVersionId(versionId);

            // 2. 创建执行上下文
            DagExecutionContext context = new DagExecutionContext(conversationId);
            context.setValue("userInput", userInput);
            context.setValue("userMessage", userInput);

            // 3. 获取起始节点
            String startNodeId = dagGraph.getStartNodeId();
            var startNode = dagGraph.getNode(startNodeId);

            if (startNode == null) {
                throw new RuntimeException("Start node not found: " + startNodeId);
            }

            log.info("开始执行DAG，起始节点: {}", startNodeId);

            // 4. 执行起始节点(简单示例，只执行第一个节点)
            String result;
            if (startNode instanceof com.zj.aiagemt.common.design.dag.DagNode) {
                @SuppressWarnings("unchecked")
                com.zj.aiagemt.common.design.dag.DagNode<DagExecutionContext, String> dagNode = (com.zj.aiagemt.common.design.dag.DagNode<DagExecutionContext, String>) startNode;
                result = dagNode.execute(context);
            } else if (startNode instanceof com.zj.aiagemt.common.design.dag.ConditionalDagNode) {
                @SuppressWarnings("unchecked")
                com.zj.aiagemt.common.design.dag.ConditionalDagNode<DagExecutionContext> conditionalNode = (com.zj.aiagemt.common.design.dag.ConditionalDagNode<DagExecutionContext>) startNode;
                Object decision = conditionalNode.execute(context);
                result = decision != null ? decision.toString() : null;
            } else {
                throw new RuntimeException("Unknown node type: " + startNode.getClass());
            }

            log.info("DAG执行完成(简单版本)，结果: {}", result);

            return result;

        } catch (Exception e) {
            log.error("DAG执行失败", e);
            throw new RuntimeException("DAG execution failed: " + e.getMessage(), e);
        }
    }
}
