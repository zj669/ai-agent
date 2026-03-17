package com.zj.aiagent.infrastructure.workflow.executor;

import com.zj.aiagent.domain.knowledge.service.KnowledgeRetrievalService;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

                String query = extractQuery(resolvedInputs);
                if (query == null || query.isEmpty()) {
                    return NodeExecutionResult.failed(
                        "未获取到查询文本，请在输入中配置 query 字段"
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

    /**
     * 兼容历史图配置：
     * 1. 优先读取标准字段 query
     * 2. 兼容旧字段 user_input
     * 3. 兜底读取第一个非系统的非空字符串输入
     * 4. 最后回退到执行上下文的全局 inputs（query/input/message）
     */
    private String extractQuery(Map<String, Object> resolvedInputs) {
        if (resolvedInputs == null || resolvedInputs.isEmpty()) {
            return null;
        }

        String query = asNonBlankString(resolvedInputs.get("query"));
        if (query != null) {
            return query;
        }

        query = asNonBlankString(resolvedInputs.get("user_input"));
        if (query != null) {
            return query;
        }

        query = resolvedInputs
            .entrySet()
            .stream()
            .filter(entry -> !entry.getKey().startsWith("__"))
            .map(Map.Entry::getValue)
            .map(this::asNonBlankString)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        if (query != null) {
            return query;
        }

        Object contextObj = resolvedInputs.get("__context__");
        if (
            contextObj instanceof ExecutionContext context &&
            context.getInputs() != null
        ) {
            query = asNonBlankString(context.getInputs().get("query"));
            if (query != null) {
                return query;
            }

            query = asNonBlankString(context.getInputs().get("input"));
            if (query != null) {
                return query;
            }

            return asNonBlankString(context.getInputs().get("message"));
        }

        return null;
    }

    private String asNonBlankString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
