package com.zj.aiagent.domain.knowledge.port;

import java.io.InputStream;
import java.util.List;

/**
 * 文档解析端口
 * 将文件流解析为文本片段列表
 */
public interface DocumentReaderPort {

    /**
     * 解析文档内容
     *
     * @param inputStream 文件输入流
     * @param filename    文件名（用于判断类型）
     * @return 解析后的文本片段列表
     */
    List<String> readDocument(InputStream inputStream, String filename);
}
