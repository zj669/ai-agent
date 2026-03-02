package com.zj.aiagent.infrastructure.workflow.executor;

import com.zj.aiagent.domain.knowledge.service.KnowledgeRetrievalService;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeNodeExecutorStrategy implements NodeExecutorStrategy {

    private final KnowledgeRetrievalService knowledgeRetrievalService;

    @Override
    public NodeType getSupportedType() {
        return NodeType.KNOWLEDGE;
    }

    @Override
    public CompletableFuture<NodeExecutionResult> executeAsync(
            Node node,
            Map<String, Object> resolvedInputs,
            StreamPublisher streamPublisher) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                NodeConfig config = node.getConfig();
                String datasetId = config.getString("knowledge_dataset_id");
                String strategy = config.getString("search_strategy");
                Long topK = config.getLong("knowledge_top_k");

                if (datasetId == null || datasetId.isEmpty()) {
                    return NodeExecutionResult.failed("未配置知识库，请在节点配置中选择知识库");
                }

                // 从已解析的输入获取查询文本
                String query = null;
                if (resolvedInputs != null && resolvedInputs.containsKey("query")) {
                    query = String.valueOf(resolvedInputs.get("query"));
                }
                if (query == null || query.isEmpty()) {
                    // 尝试从 user_input 获取
                    Object userInput = resolvedInputs != null ? resolvedInputs.get("user_input") : null;
                    if (userInput != null) {
                        query = String.valueOf(userInput);
                    }
                }
                if (query == null || query.isEmpty()) {
                    return NodeExecutionResult.failed("未获取到查询文本，请在输入中配置 query 字段");
                }

                int k = (topK != null && topK > 0) ? topK.intValue() : 5;
                if (strategy == null || strategy.isEmpty()) {
                    strategy = "SEMANTIC";
                }

                log.info("知识库检索: datasetId={}, strategy={}, topK={}, query={}",
                        datasetId, strategy, k,
                        query.length() > 50 ? query.substring(0, 50) + "..." : query);

                List<String> results = knowledgeRetrievalService.retrieveByDataset(datasetId, query, k);

                log.info("知识库检索完成: 返回 {} 条结果", results.size());

                return NodeExecutionResult.success(Map.of("knowledge_list", results));

            } catch (Exception e) {
                log.error("知识库检索失败", e);
                return NodeExecutionResult.failed("知识库检索失败: " + e.getMessage());
            }
        });
    }
}
