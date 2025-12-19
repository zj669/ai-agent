package com.zj.aiagemt.service.rag.split;

import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RecursiveBlocksSplit extends TextSplitter {

    // 块大小配置
    private Integer CHUNK_SIZE = 200;
    private Integer CHUNK_OVERLAP = 20;

    // 分隔符优先级列表(从高到低)
    private final String[] SEPARATORS = {
            "\n\n", // 段落分隔
            "\n", // 行分隔
            "。", // 中文句号
            "！", // 中文感叹号
            "？", // 中文问号
            "；", // 中文分号
            "\\.", // 英文句号
            "!", // 英文感叹号
            "\\?", // 英文问号
            ";", // 英文分号
            " ", // 空格(单词分隔)
            "" // 字符级分隔(最后的兜底策略)
    };

    @Override
    protected List<String> splitText(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        recursiveSplit(text, 0, result);
        return result;
    }

    /**
     * 递归分割文本
     * 
     * @param text           待分割的文本
     * @param separatorIndex 当前使用的分隔符索引
     * @param result         结果列表
     */
    private void recursiveSplit(String text, int separatorIndex, List<String> result) {
        // 如果文本长度小于等于块大小,直接添加到结果
        if (text.length() <= CHUNK_SIZE) {
            if (!text.isEmpty()) {
                result.add(text);
            }
            return;
        }

        // 如果已经用完所有分隔符,强制按字符分割
        if (separatorIndex >= SEPARATORS.length) {
            forceCharacterSplit(text, result);
            return;
        }

        String separator = SEPARATORS[separatorIndex];

        // 如果是空字符串分隔符(字符级分隔)
        if (separator.isEmpty()) {
            forceCharacterSplit(text, result);
            return;
        }

        // 使用当前分隔符分割文本
        List<String> splits = splitBySeparator(text, separator);

        // 如果无法分割(没有找到分隔符),尝试下一个分隔符
        if (splits.size() == 1) {
            recursiveSplit(text, separatorIndex + 1, result);
            return;
        }

        // 合并分割后的块,确保每个块不超过CHUNK_SIZE
        mergeAndSplit(splits, separator, separatorIndex, result);
    }

    /**
     * 使用指定分隔符分割文本
     */
    private List<String> splitBySeparator(String text, String separator) {
        List<String> parts = new ArrayList<>();
        Pattern pattern = Pattern.compile(separator);
        Matcher matcher = pattern.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            String part = text.substring(lastEnd, matcher.start());
            String sep = matcher.group();
            if (!part.isEmpty() || !sep.isEmpty()) {
                parts.add(part + sep); // 保留分隔符
            }
            lastEnd = matcher.end();
        }

        // 添加最后一部分
        if (lastEnd < text.length()) {
            parts.add(text.substring(lastEnd));
        }

        // 如果没有分割成功,返回原文本
        if (parts.isEmpty()) {
            parts.add(text);
        }

        return parts;
    }

    /**
     * 合并小块并递归处理大块
     */
    private void mergeAndSplit(List<String> splits, String separator, int separatorIndex, List<String> result) {
        StringBuilder currentChunk = new StringBuilder();

        for (String split : splits) {
            // 如果单个分割块就超过大小,需要递归处理
            if (split.length() > CHUNK_SIZE) {
                // 先保存当前累积的块
                if (currentChunk.length() > 0) {
                    result.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
                // 递归处理超大块
                recursiveSplit(split, separatorIndex + 1, result);
                continue;
            }

            // 如果加上当前分割块会超过大小
            if (currentChunk.length() + split.length() > CHUNK_SIZE) {
                // 保存当前块
                if (currentChunk.length() > 0) {
                    result.add(currentChunk.toString());
                }
                // 开始新块,考虑重叠
                currentChunk = new StringBuilder();
                if (result.size() > 0 && CHUNK_OVERLAP > 0) {
                    String lastChunk = result.get(result.size() - 1);
                    int overlapStart = Math.max(0, lastChunk.length() - CHUNK_OVERLAP);
                    currentChunk.append(lastChunk.substring(overlapStart));
                }
            }

            currentChunk.append(split);
        }

        // 添加最后一个块
        if (currentChunk.length() > 0) {
            result.add(currentChunk.toString());
        }
    }

    /**
     * 强制按字符分割(当所有分隔符都无法使用时)
     */
    private void forceCharacterSplit(String text, List<String> result) {
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            result.add(text.substring(start, end));

            // 考虑重叠
            start = start + CHUNK_SIZE - CHUNK_OVERLAP;
            if (start >= text.length()) {
                break;
            }
        }
    }
}
