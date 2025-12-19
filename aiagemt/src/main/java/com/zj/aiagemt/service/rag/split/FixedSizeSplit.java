package com.zj.aiagemt.service.rag.split;

import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FixedSizeSplit extends TextSplitter {
    private Integer CHUNK_SIZE = 200;
    private Integer CHUNK_OVERLAP = 20;

    @Override
    protected List<String> splitText(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int textLength = text.length();

        // 如果文本长度小于等于块大小,直接返回整个文本
        if (textLength <= CHUNK_SIZE) {
            chunks.add(text);
            return chunks;
        }

        int start = 0;
        while (start < textLength) {
            // 计算当前块的结束位置
            int end = Math.min(start + CHUNK_SIZE, textLength);

            // 提取当前块
            String chunk = text.substring(start, end);
            chunks.add(chunk);

            // 如果已经到达文本末尾,退出循环
            if (end >= textLength) {
                break;
            }

            // 计算下一个块的起始位置(考虑重叠)
            start = start + CHUNK_SIZE - CHUNK_OVERLAP;

            // 确保不会因为重叠过大而导致无限循环
            if (CHUNK_OVERLAP >= CHUNK_SIZE) {
                start = end;
            }
        }

        return chunks;
    }
}
