package com.zj.aiagemt.service.rag.split;

import com.zj.aiagemt.service.rag.split.similarity.SimilarityCalculator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义分块器
 * 基于句子语义相似度进行智能分块
 * 1. 将文本按句号分割成句子
 * 2. 使用 EmbeddingModel 为每个句子生成向量
 * 3. 计算相邻句子的相似度
 * 4. 相似度高于阈值的句子合并到同一块
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SemanticBlockingSplit extends TextSplitter {

    private final EmbeddingModel embeddingModel;
    private final SimilarityCalculator similarityCalculator;

    // 相似度阈值，高于此值的句子会被合并
    private double similarityThreshold = 0.7;

    // 最大块大小（字符数）
    private int maxChunkSize = 1000;

    // 句子分割正则表达式
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
            "[^。！？.!?\\n]+[。！？.!?\\n]+");

    public SemanticBlockingSplit(EmbeddingModel embeddingModel,
            SimilarityCalculator similarityCalculator) {
        this.embeddingModel = embeddingModel;
        this.similarityCalculator = similarityCalculator;
    }

    @Override
    protected List<String> splitText(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        // 1. 分割成句子
        List<String> sentences = splitIntoSentences(text);
        if (sentences.isEmpty()) {
            return List.of(text);
        }

        // 如果只有一个句子
        if (sentences.size() == 1) {
            return sentences;
        }

        // 2. 批量生成句子向量
        List<List<Double>> sentenceEmbeddings = generateEmbeddings(sentences);

        // 3. 基于相似度合并句子
        return mergeBySemanticSimilarity(sentences, sentenceEmbeddings);
    }

    /**
     * 将文本分割成句子
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(text);

        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }

        // 如果没有匹配到任何句子，返回原文本
        if (sentences.isEmpty() && !text.trim().isEmpty()) {
            sentences.add(text.trim());
        }

        return sentences;
    }

    /**
     * 批量生成句子的向量表示
     */
    private List<List<Double>> generateEmbeddings(List<String> sentences) {
        // 批量生成向量 - embed 方法接受 List<String>
        List<float[]> embeddings = embeddingModel.embed(sentences);

        // 将 float[] 转换为 List<Double>
        List<List<Double>> result = new ArrayList<>();
        for (float[] embedding : embeddings) {
            List<Double> doubleList = new ArrayList<>();
            for (float f : embedding) {
                doubleList.add((double) f);
            }
            result.add(doubleList);
        }

        return result;
    }

    /**
     * 基于语义相似度合并句子
     */
    private List<String> mergeBySemanticSimilarity(List<String> sentences,
            List<List<Double>> embeddings) {
        List<String> chunks = new ArrayList<>();

        if (sentences.isEmpty()) {
            return chunks;
        }

        StringBuilder currentChunk = new StringBuilder();
        List<Double> currentEmbedding = embeddings.get(0);
        currentChunk.append(sentences.get(0));

        for (int i = 1; i < sentences.size(); i++) {
            String nextSentence = sentences.get(i);
            List<Double> nextEmbedding = embeddings.get(i);

            // 计算当前块与下一个句子的相似度
            double similarity = similarityCalculator.calculate(currentEmbedding, nextEmbedding);

            // 检查是否应该合并
            boolean shouldMerge = similarity >= similarityThreshold
                    && (currentChunk.length() + nextSentence.length()) <= maxChunkSize;

            if (shouldMerge) {
                // 合并到当前块
                currentChunk.append(nextSentence);
                // 更新当前块的向量表示为下一个句子的向量
                // （也可以使用平均向量，但这里简化处理）
                currentEmbedding = nextEmbedding;
            } else {
                // 保存当前块，开始新块
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder(nextSentence);
                currentEmbedding = nextEmbedding;
            }
        }

        // 添加最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }
}
