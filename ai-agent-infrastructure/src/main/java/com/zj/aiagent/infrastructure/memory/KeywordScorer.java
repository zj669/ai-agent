package com.zj.aiagent.infrastructure.memory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 轻量级关键词评分器
 * 对中文/英文文本做简单分词，计算 query 词在文档中的命中率
 * 用于在 Milvus 2.3 不支持原生 BM25 时提供应用层关键词检索能力
 */
public final class KeywordScorer {

    private KeywordScorer() {}

    // 按标点、空格、常见分隔符分割
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\s\\p{Punct}\uff0c\u3002\uff01\uff1f\u3001\uff1b\uff1a\u201c\u201d\u2018\u2019\uff08\uff09\u3010\u3011\u300a\u300b\u3000]+");

    // 中文停用词（最小集合）
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "他", "她", "它", "们", "那", "被", "从", "把", "对",
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "shall", "can", "need", "dare", "ought",
            "to", "of", "in", "for", "on", "with", "at", "by", "from", "as",
            "into", "through", "during", "before", "after", "and", "but", "or",
            "not", "no", "nor", "so", "if", "then", "than", "too", "very",
            "this", "that", "these", "those", "it", "its"
    );

    /**
     * 对文本进行分词（简单实现：按标点空格分割 + 中文单字/双字切分）
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        // 先按标点空格分割
        String[] segments = SPLIT_PATTERN.split(text.toLowerCase().trim());

        for (String segment : segments) {
            if (segment.isEmpty()) continue;

            // 判断是否包含中文字符
            if (containsChinese(segment)) {
                // 中文：bigram 切分
                for (int i = 0; i < segment.length() - 1; i++) {
                    String bigram = segment.substring(i, i + 2);
                    if (!STOP_WORDS.contains(bigram)) {
                        tokens.add(bigram);
                    }
                }
                // 也保留单字（长度>1的segment）
                if (segment.length() == 1 && !STOP_WORDS.contains(segment)) {
                    tokens.add(segment);
                }
            } else {
                // 英文：整词
                if (segment.length() > 1 && !STOP_WORDS.contains(segment)) {
                    tokens.add(segment);
                }
            }
        }

        return tokens;
    }

    /**
     * 计算关键词相关性分数
     * 基于 query 分词后在 document 中的命中率
     *
     * @return 0.0 ~ 1.0 的分数
     */
    public static double score(String query, String document) {
        if (query == null || document == null || query.isEmpty() || document.isEmpty()) {
            return 0.0;
        }

        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return 0.0;
        }

        String docLower = document.toLowerCase();
        List<String> docTokens = tokenize(document);
        Set<String> docTokenSet = new HashSet<>(docTokens);

        // 计算命中的 query token 数量
        long hits = queryTokens.stream()
                .filter(token -> docTokenSet.contains(token) || docLower.contains(token))
                .count();

        return (double) hits / queryTokens.size();
    }

    private static boolean containsChinese(String text) {
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }
}