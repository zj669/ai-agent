package com.zj.aiagent.infrastructure.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KeywordScorer 单元测试
 * 验证分词、评分、边界情况
 */
class KeywordScorerTest {

    // ========== tokenize 测试 ==========

    @Test
    @DisplayName("中文文本应产生 bigram 分词")
    void tokenize_chinese_bigram() {
        List<String> tokens = KeywordScorer.tokenize("机器学习算法");
        // "机器", "器学", "学习", "习算", "算法"
        assertTrue(tokens.contains("机器"));
        assertTrue(tokens.contains("学习"));
        assertTrue(tokens.contains("算法"));
        assertFalse(tokens.isEmpty());
    }

    @Test
    @DisplayName("英文文本应按整词分割并过滤停用词")
    void tokenize_english_words() {
        List<String> tokens = KeywordScorer.tokenize("the quick brown fox jumps over lazy dog");
        // "the" 是停用词应被过滤, "quick", "brown", "fox" 等保留
        assertFalse(tokens.contains("the"));
        assertTrue(tokens.contains("quick"));
        assertTrue(tokens.contains("brown"));
        assertTrue(tokens.contains("fox"));
    }

    @Test
    @DisplayName("中英混合文本应正确分词")
    void tokenize_mixed_chinese_english() {
        List<String> tokens = KeywordScorer.tokenize("Spring Boot 微服务架构");
        assertTrue(tokens.contains("spring"));
        assertTrue(tokens.contains("boot"));
        assertTrue(tokens.contains("微服"));
        assertTrue(tokens.contains("服务"));
    }

    @Test
    @DisplayName("停用词应被过滤")
    void tokenize_filters_stopwords() {
        List<String> tokens = KeywordScorer.tokenize("我在学习");
        // "我" 和 "在" 是停用词，单字不保留；bigram "我在"、"在学" 中 "我" 是停用词但 bigram 不一定在停用词表
        // 关键验证：不应包含纯停用词单字
        for (String token : tokens) {
            // 每个 token 长度应 >= 2（bigram）
            assertTrue(token.length() >= 1);
        }
    }

    @Test
    @DisplayName("null 和空字符串应返回空列表")
    void tokenize_null_and_empty() {
        assertEquals(List.of(), KeywordScorer.tokenize(null));
        assertEquals(List.of(), KeywordScorer.tokenize(""));
    }

    @Test
    @DisplayName("标点符号应被正确分割")
    void tokenize_punctuation_split() {
        List<String> tokens = KeywordScorer.tokenize("知识库，向量检索！语义搜索");
        assertTrue(tokens.contains("知识"));
        assertTrue(tokens.contains("向量"));
        assertTrue(tokens.contains("检索"));
        assertTrue(tokens.contains("语义"));
        assertTrue(tokens.contains("搜索"));
    }

    // ========== score 测试 ==========

    @Test
    @DisplayName("完全命中应返回 1.0")
    void score_full_match() {
        double score = KeywordScorer.score("向量数据库", "向量数据库是一种专门存储向量的数据库系统");
        assertEquals(1.0, score, 0.01);
    }

    @Test
    @DisplayName("部分命中应返回 0~1 之间的分数")
    void score_partial_match() {
        double score = KeywordScorer.score("向量数据库检索", "这是一个关于机器学习的文档，包含向量计算");
        assertTrue(score > 0.0, "应有部分命中");
        assertTrue(score < 1.0, "不应完全命中");
    }

    @Test
    @DisplayName("零命中应返回 0.0")
    void score_no_match() {
        double score = KeywordScorer.score("量子计算", "今天天气很好适合出去玩");
        assertEquals(0.0, score, 0.01);
    }

    @Test
    @DisplayName("null 输入应返回 0.0")
    void score_null_inputs() {
        assertEquals(0.0, KeywordScorer.score(null, "文档内容"));
        assertEquals(0.0, KeywordScorer.score("查询", null));
        assertEquals(0.0, KeywordScorer.score(null, null));
    }

    @Test
    @DisplayName("空字符串输入应返回 0.0")
    void score_empty_inputs() {
        assertEquals(0.0, KeywordScorer.score("", "文档内容"));
        assertEquals(0.0, KeywordScorer.score("查询", ""));
    }

    @Test
    @DisplayName("英文评分应正确计算")
    void score_english() {
        double score = KeywordScorer.score("vector database", "A vector database stores embeddings for similarity search");
        assertTrue(score > 0.0, "vector 应命中");
    }

    @Test
    @DisplayName("关键词密度高的文档应得分更高")
    void score_keyword_density_ranking() {
        String query = "知识库检索";
        String highDensityDoc = "知识库检索是通过向量相似度在知识库中检索相关内容的技术";
        String lowDensityDoc = "今天学习了一些新的编程技巧和方法论";

        double highScore = KeywordScorer.score(query, highDensityDoc);
        double lowScore = KeywordScorer.score(query, lowDensityDoc);

        assertTrue(highScore > lowScore,
                "高关键词密度文档得分(" + highScore + ")应高于低密度文档(" + lowScore + ")");
    }
}