package com.zj.aiagemt.service.rag.split;

import com.zj.aiagemt.service.rag.split.similarity.SimilarityCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * SemanticBlockingSplit 单元测试
 * 测试基于语义相似度的分块功能
 */
class SemanticBlockingSplitTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private SimilarityCalculator similarityCalculator;

    private SemanticBlockingSplit splitter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        splitter = new SemanticBlockingSplit(embeddingModel, similarityCalculator);
    }

    @Test
    void testSplitText_EmptyText() {
        List<String> result = splitter.splitText("");
        assertTrue(result.isEmpty(), "空文本应返回空列表");
    }

    @Test
    void testSplitText_NullText() {
        List<String> result = splitter.splitText((String) null);
        assertTrue(result.isEmpty(), "null 应返回空列表");
    }

    @Test
    void testSplitText_SingleSentence() {
        // 测试单个句子
        String text = "这是一个句子。";

        // Mock embedding
        when(embeddingModel.embed(anyList())).thenReturn(
                Arrays.asList(createMockEmbedding(10)));

        List<String> result = splitter.splitText(text);

        assertEquals(1, result.size(), "单个句子应返回一个块");
        assertEquals(text, result.get(0), "应该返回原句子");
    }

    @Test
    void testSplitText_HighSimilarity_Merge() {
        // 测试高相似度句子合并
        String text = "第一句话。第二句话。第三句话。";

        // Mock embeddings for 3 sentences
        when(embeddingModel.embed(anyList())).thenReturn(Arrays.asList(
                createMockEmbedding(10),
                createMockEmbedding(10),
                createMockEmbedding(10)));

        // Mock high similarity (> 0.7) - 所有句子应该合并
        when(similarityCalculator.calculate(anyList(), anyList())).thenReturn(0.9);

        List<String> result = splitter.splitText(text);

        // 由于相似度高，应该合并成一个块
        assertTrue(result.size() <= 2, "高相似度的句子应该被合并");
        printSplit( result);
    }

    @Test
    void testSplitText_LowSimilarity_NoMerge() {
        // 测试低相似度句子不合并
        String text = "第一句话。第二句话。第三句话。";

        // Mock embeddings
        when(embeddingModel.embed(anyList())).thenReturn(Arrays.asList(
                createMockEmbedding(10),
                createMockEmbedding(10),
                createMockEmbedding(10)));

        // Mock low similarity (< 0.7) - 句子应该分开
        when(similarityCalculator.calculate(anyList(), anyList())).thenReturn(0.3);

        List<String> result = splitter.splitText(text);

        // 由于相似度低，每个句子应该是独立的块
        assertEquals(3, result.size(), "低相似度的句子应该分开");
        printSplit( result);
    }

    @Test
    void testSplitText_MixedSimilarity() {
        // 测试混合相似度
        String text = "第一句话。第二句话。第三句话。第四句话。";

        // Mock embeddings
        when(embeddingModel.embed(anyList())).thenReturn(Arrays.asList(
                createMockEmbedding(10),
                createMockEmbedding(10),
                createMockEmbedding(10),
                createMockEmbedding(10)));

        // Mock varying similarity: 1-2高, 2-3低, 3-4高
        when(similarityCalculator.calculate(anyList(), anyList()))
                .thenReturn(0.9) // 1-2 high
                .thenReturn(0.3) // 2-3 low
                .thenReturn(0.9); // 3-4 high

        List<String> result = splitter.splitText(text);

        // 应该有多个块
        assertTrue(result.size() >= 2, "混合相似度应产生多个块");
        printSplit( result);
    }

    @Test
    void testSplitText_ExceedsMaxChunkSize() {
        // 测试超过最大块大小的情况
        // 构造一个很长的句子
        String longSentence = "这是一个很长的句子".repeat(60) + "。"; // 约720字符
        String text = longSentence + longSentence; // 约1440字符，超过1000

        // Mock embeddings
        when(embeddingModel.embed(anyList())).thenReturn(Arrays.asList(
                createMockEmbedding(10),
                createMockEmbedding(10)));

        // Mock high similarity
        when(similarityCalculator.calculate(anyList(), anyList())).thenReturn(0.9);

        List<String> result = splitter.splitText(text);

        // 即使相似度高，由于超过maxChunkSize，也应该分成两个块
        assertTrue(result.size() >= 2, "超过最大块大小应该强制分割");

        for (String chunk : result) {
            assertTrue(chunk.length() <= 1000, "每个块不应超过maxChunkSize");
        }
    }

    @Test
    void testSplitText_ChineseSentences() {
        // 测试中文句子
        String text = "人工智能是未来的趋势。机器学习正在改变世界。深度学习是关键技术。";

        when(embeddingModel.embed(anyList())).thenReturn(Arrays.asList(
                createMockEmbedding(10),
                createMockEmbedding(10),
                createMockEmbedding(10)));

        when(similarityCalculator.calculate(anyList(), anyList())).thenReturn(0.8);

        List<String> result = splitter.splitText(text);

        assertFalse(result.isEmpty(), "应该成功处理中文句子");
        printSplit( result);
    }

    @Test
    void testSplitText_EnglishSentences() {
        // 测试英文句子
        String text = "AI is the future. Machine learning is powerful. Deep learning is key.";

        when(embeddingModel.embed(anyList())).thenReturn(Arrays.asList(
                createMockEmbedding(10),
                createMockEmbedding(10),
                createMockEmbedding(10)));

        when(similarityCalculator.calculate(anyList(), anyList())).thenReturn(0.8);

        List<String> result = splitter.splitText(text);

        assertFalse(result.isEmpty(), "应该成功处理英文句子");
    }

    @Test
    void testSplitText_WithNewlines() {
        // 测试包含换行符的文本
        String text = "第一句话。\n第二句话。\n第三句话。";

        when(embeddingModel.embed(anyList())).thenReturn(Arrays.asList(
                createMockEmbedding(10),
                createMockEmbedding(10),
                createMockEmbedding(10)));

        when(similarityCalculator.calculate(anyList(), anyList())).thenReturn(0.5);

        List<String> result = splitter.splitText(text);

        assertFalse(result.isEmpty(), "应该正确处理包含换行符的文本");
    }

    @Test
    void testSimilarityThreshold() {
        // 测试相似度阈值配置
        splitter.setSimilarityThreshold(0.8);
        assertEquals(0.8, splitter.getSimilarityThreshold(), "应该正确设置相似度阈值");
    }

    @Test
    void testMaxChunkSize() {
        // 测试最大块大小配置
        splitter.setMaxChunkSize(500);
        assertEquals(500, splitter.getMaxChunkSize(), "应该正确设置最大块大小");
    }

    /**
     * 创建模拟的向量
     */
    private float[] createMockEmbedding(int dimension) {
        float[] embedding = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            embedding[i] = (float) Math.random();
        }
        return embedding;
    }

    private void printSplit(List<String> result){
        for (String chunk : result) {
            System.out.println(chunk);
            System.out.println("==================");
        }
    }
}
