package com.zj.aiagemt.service.rag.split;

import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.Arrays;
import java.util.List;

public class ParagraphSplit extends TextSplitter {
    @Override
    protected List<String> splitText(String text) {
        // 使用正则表达式匹配句号、逗号、换行符（包括Windows和Unix风格的换行符）作为分隔符
        // 正则表达式 "[。，\\r?\\n|\\r]" 表示：句号、逗号、可选的回车符后跟换行符（\r\n），或单独的换行符（\n），或单独的回车符（\r）
        String[] parts = text.split("[。\\r?\\n|\\r]+");

        // 移除可能出现的空字符串（例如，如果分隔符连续出现或出现在开头/结尾）
        // 并将数组转换为List
        return Arrays.stream(parts)
                .filter(part -> !part.trim().isEmpty()) // 过滤掉空白字符串
                .toList();
    }
}
