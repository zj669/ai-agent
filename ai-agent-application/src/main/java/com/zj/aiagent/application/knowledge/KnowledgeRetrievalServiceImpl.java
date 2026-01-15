package com.zj.aiagent.application.knowledge;

import com.zj.aiagent.domain.knowledge.service.KnowledgeRetrievalService;
import com.zj.aiagent.domain.memory.port.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识检索服务实现
 * 实现领域层的 KnowledgeRetrievalService 接口
 * 
 * 职责：
 * - 为 SchedulerService 提供长期记忆（LTM）检索能力
 * - 使用 Metadata Filter 确保只检索属于该 Agent 的知识
 * - 支持按 Dataset 检索（测试用）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private final VectorStore vectorStore;

    /**
     * 根据 Agent ID 检索知识
     * 自动过滤出属于该 Agent 的知识库内容
     * 
     * @param agentId Agent ID（用于权限隔离）
     * @param query   用户查询文本
     * @param topK    返回结果数量
     * @return 相关知识片段列表
     */
    @Override
    public List<String> retrieve(Long agentId, String query, int topK) {
        log.debug("检索 Agent 知识库: agentId={}, query='{}', topK={}",
                agentId,
                query.length() > 50 ? query.substring(0, 50) + "..." : query,
                topK);

        try {
            // 构建 SearchRequest，使用 agent_id 过滤
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression("agentId == " + agentId) // 注意：Milvus 过滤表达式语法
                    .build();

            // 执行检索
            List<Document> results = vectorStore.similaritySearch(request);

            log.debug("检索到 {} 条知识片段", results.size());

            // 提取文本内容
            return results.stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("知识检索失败: agentId={}, query={}", agentId, query, e);
            // 返回空列表，避免阻塞主流程
            return List.of();
        }
    }

    /**
     * 根据 Dataset ID 检索知识（测试用）
     * 
     * @param datasetId 知识库 ID
     * @param query     查询文本
     * @param topK      返回结果数量
     * @return 相关知识片段列表
     */
    @Override
    public List<String> retrieveByDataset(String datasetId, String query, int topK) {
        log.debug("按知识库检索: datasetId={}, query='{}', topK={}",
                datasetId,
                query.length() > 50 ? query.substring(0, 50) + "..." : query,
                topK);

        try {
            // 构建 SearchRequest，使用 datasetId 过滤
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression("datasetId == '" + datasetId + "'") // 字符串需要加引号
                    .build();

            // 执行检索
            List<Document> results = vectorStore.similaritySearch(request);

            log.debug("检索到 {} 条知识片段", results.size());

            // 提取文本内容
            return results.stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("知识库检索失败: datasetId={}, query={}", datasetId, query, e);
            return List.of();
        }
    }
}
