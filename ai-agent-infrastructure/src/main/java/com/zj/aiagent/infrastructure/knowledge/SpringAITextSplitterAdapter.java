package com.zj.aiagent.infrastructure.knowledge;

import com.zj.aiagent.domain.knowledge.port.TextSplitterPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI 文本分块适配器
 * 封装 TokenTextSplitter，提供智能分块能力
 */
@Slf4j
@Component
public class SpringAITextSplitterAdapter implements TextSplitterPort {

    /**
     * 将文档列表分块（Spring AI Document 版本，内部使用）
     */
    public List<Document> splitDocuments(List<Document> documents, int chunkSize, int overlap) {
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

    @Override
    public List<String> split(List<String> texts, int chunkSize, int overlap) {
        List<Document> documents = texts.stream()
                .map(Document::new)
                .collect(Collectors.toList());
        List<Document> chunks = splitDocuments(documents, chunkSize, overlap);
        return chunks.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }
}
