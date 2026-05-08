package com.zj.aiagent.infrastructure.workflow.executor;

import com.zj.aiagent.domain.knowledge.service.KnowledgeRetrievalService;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        StreamPublisher streamPublisher
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                NodeConfig config = node.getConfig();
                String datasetId = config.getString("knowledge_dataset_id");
                String strategy = config.getString("search_strategy");
                Long topK = config.getLong("knowledge_top_k");

                if (datasetId == null || datasetId.isEmpty()) {
                    return NodeExecutionResult.failed(
                        "未配置知识库，请在节点配置中选择知识库"
                    );
                }

                String query = asNonBlankString(resolvedInputs.get("query"));
                if (query == null || query.isEmpty()) {
                    Object ref = node.getInputs() != null
                        ? node.getInputs().get("query")
                        : null;
                    return NodeExecutionResult.failed(
                        "KNOWLEDGE 入参解析失败：node=" +
                            node.getNodeId() +
                            " input=query ref=" +
                            (ref != null ? ref : "<未配置>") +
                            " reason=必填参数为空"
                    );
                }

                int k = (topK != null && topK > 0) ? topK.intValue() : 5;
                if (strategy == null || strategy.isEmpty()) {
                    strategy = "SEMANTIC";
                }

                log.info(
                    "知识库检索: datasetId={}, strategy={}, topK={}, query={}",
                    datasetId,
                    strategy,
                    k,
                    query.length() > 50 ? query.substring(0, 50) + "..." : query
                );

                List<String> results =
                    knowledgeRetrievalService.retrieveByDataset(
                        datasetId,
                        query,
                        k,
                        strategy
                    );

                log.info("知识库检索完成: 返回 {} 条结果", results.size());

                return NodeExecutionResult.success(
                    Map.of("knowledge_list", results)
                );
            } catch (Exception e) {
                log.error("知识库检索失败", e);
                return NodeExecutionResult.failed(
                    "知识库检索失败: " + e.getMessage()
                );
            }
        });
    }

    private String asNonBlankString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
