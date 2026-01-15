package com.zj.aiagent.infrastructure.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI 文档读取适配器
 * 封装 TikaDocumentReader，支持 PDF/DOCX/TXT/MD 等多种格式
 */
@Slf4j
@Component
public class SpringAIDocumentReaderAdapter {

    /**
     * 读取文档并解析为 Spring AI Document 列表
     * 
     * @param resource 文件资源（可以是 FileSystemResource 或 InputStreamResource）
     * @return Document 列表
     */
    public List<Document> readDocuments(Resource resource) {
        try {
            log.info("Reading document with Spring AI TikaDocumentReader: {}",
                    resource.getFilename());

            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.get();

            log.info("Successfully read {} documents from file: {}",
                    documents.size(), resource.getFilename());

            return documents;

        } catch (Exception e) {
            log.error("Failed to read document: {}", resource.getFilename(), e);
            throw new RuntimeException("文档读取失败: " + e.getMessage(), e);
        }
    }
}
