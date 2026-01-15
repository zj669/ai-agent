package com.zj.aiagent.infrastructure.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI 文本分块适配器
 * 封装 TokenTextSplitter，提供智能分块能力
 */
@Slf4j
@Component
public class SpringAITextSplitterAdapter {

    /**
     * 将文档列表分块
     * 
     * @param documents 待分块的文档列表
     * @param chunkSize 分块大小（Token 数量）
     * @param overlap   重叠大小（Token 数量）
     * @return 分块后的 Document 列表
     */
    public List<Document> split(List<Document> documents, int chunkSize, int overlap) {
        try {
            log.info("Splitting documents: count={}, chunkSize={}, overlap={}",
                    documents.size(), chunkSize, overlap);

            TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, overlap, 5, 1000, true);
            List<Document> chunks = splitter.split(documents);

            log.info("Successfully split into {} chunks", chunks.size());

            return chunks;

        } catch (Exception e) {
            log.error("Failed to split documents", e);
            throw new RuntimeException("文档分块失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用默认配置分块（500 Token, 50 overlap）
     * 
     * @param documents 待分块的文档列表
     * @return 分块后的 Document 列表
     */
    public List<Document> splitWithDefaults(List<Document> documents) {
        return split(documents, 500, 50);
    }
}
