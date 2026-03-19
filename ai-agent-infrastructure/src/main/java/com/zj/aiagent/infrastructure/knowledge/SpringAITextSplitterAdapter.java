package com.zj.aiagent.infrastructure.knowledge;

import com.zj.aiagent.domain.knowledge.valobj.ChunkingConfig;
import com.zj.aiagent.domain.knowledge.valobj.ChunkingStrategy;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

/**
 * Spring AI 文本分块适配器
 * 封装 TokenTextSplitter，提供固定分块能力
 */
@Slf4j
@Component
public class SpringAITextSplitterAdapter implements ChunkingStrategySplitter {

    /**
     * 将文档列表分块（Spring AI Document 版本，内部使用）
     */
    public List<Document> splitDocuments(
        List<Document> documents,
        int chunkSize,
        int overlap
    ) {
        try {
            log.info(
                "Splitting documents: count={}, chunkSize={}, overlap={}",
                documents.size(),
                chunkSize,
                overlap
            );

            TokenTextSplitter splitter = new TokenTextSplitter(
                chunkSize,
                overlap,
                5,
                1000,
                true
            );
            List<Document> chunks = splitter.split(documents);

            log.info("Successfully split into {} chunks", chunks.size());

            return chunks;
        } catch (Exception e) {
            log.error("Failed to split documents", e);
            throw new RuntimeException("文档分块失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(ChunkingStrategy strategy) {
        return strategy == ChunkingStrategy.FIXED;
    }

    @Override
    public List<String> split(List<String> texts, ChunkingConfig config) {
        ChunkingConfig normalized = config.normalized();
        List<Document> documents = texts
            .stream()
            .map(Document::new)
            .collect(Collectors.toList());
        List<Document> chunks = splitDocuments(
            documents,
            normalized.getChunkSize(),
            normalized.getChunkOverlap()
        );
        return chunks
            .stream()
            .map(Document::getText)
            .collect(Collectors.toList());
    }
}
