package com.zj.aiagemt.service.rag.split;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RecursiveBlocksSplit 单元测试
 * 测试递归分块功能
 */
class RecursiveBlocksSplitTest {

    private RecursiveBlocksSplit splitter;

    @BeforeEach
    void setUp() {
        splitter = new RecursiveBlocksSplit();
    }

    @Test
    void testSplitText_EmptyText() {
        List<String> result = splitter.splitText("");
        assertTrue(result.isEmpty(), "空文本应返回空列表");
    }

    @Test
    void testSplitText_ShortText() {
        String text = "这是一段短文本。";
        List<String> result = splitter.splitText(text);

        assertEquals(1, result.size(), "短文本应返回一个块");
        assertEquals(text, result.get(0), "短文本应完整返回");
    }

    @Test
    void testSplitText_ByParagraph() {
        // 测试按段落分割
        String text = "第一段内容。\n\n第二段内容。\n\n第三段内容。";
        List<String> result = splitter.splitText(text);

        assertFalse(result.isEmpty(), "应该成功分割");
        // 由于文本较短，可能会合并段落
        assertTrue(result.size() >= 1, "应该至少有一个块");
        printSplit(result);
    }

    @Test
    void testSplitText_BySentence() {
        // 测试按句子分割（中文）
        String text = "这是第一句话。这是第二句话。这是第三句话。这是第四句话。";
        List<String> result = splitter.splitText(text);

        assertFalse(result.isEmpty(), "应该成功分割");
        for (String chunk : result) {
            assertTrue(chunk.length() <= 1000, "每个块不应超过1000字符");
        }
        printSplit(result);
    }

    @Test
    void testSplitText_ByEnglishSentence() {
        // 测试按句子分割（英文）
        String text = "This is the first sentence. This is the second sentence. This is the third sentence.";
        List<String> result = splitter.splitText(text);

        assertFalse(result.isEmpty(), "应该成功分割");
        printSplit(result);
    }

    @Test
    void testSplitText_MixedLanguage() {
        // 测试中英文混合
        String text = "这是中文句子。This is an English sentence. 另一个中文句子！Another English sentence?";
        List<String> result = splitter.splitText(text);

        assertFalse(result.isEmpty(), "应该成功分割混合语言文本");

        // 拼接所有块，应该包含原始内容
        String combined = String.join("", result);
        assertTrue(combined.contains("这是中文句子"), "应该保留中文内容");
        assertTrue(combined.contains("This is an English sentence"), "应该保留英文内容");

        printSplit(result);
    }

    @Test
    void testSplitText_LongParagraph() {
        // 测试超长段落（需要递归分割）
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("这是第").append(i).append("句话。");
        }
        String text = sb.toString();

        List<String> result = splitter.splitText(text);

        assertTrue(result.size() > 1, "超长段落应该被分成多个块");

        for (String chunk : result) {
            assertTrue(chunk.length() <= 1000, "每个块不应超过1000字符");
        }
        printSplit(result);
    }

    @Test
    void testSplitText_RecursiveSplitting() {
        // 测试递归分割：创建一个没有段落分隔符的长文本
        String longSentence = "这是一个很长的句子".repeat(100); // 约1000字符
        String text = longSentence + "。" + longSentence + "。" + longSentence + "。";

        List<String> result = splitter.splitText(text);

        assertTrue(result.size() > 1, "应该递归分割长文本");

        for (String chunk : result) {
            assertTrue(chunk.length() <= 1000, "每个块不应超过1000字符");
        }
        printSplit(result);
    }

    @Test
    void testSplitText_WithNewlines() {
        // 测试包含换行符的文本
        String text = "第一行\n第二行\n第三行\n第四行\n第五行";
        List<String> result = splitter.splitText(text);

        assertFalse(result.isEmpty(), "应该成功分割包含换行符的文本");
        printSplit(result);
    }

    @Test
    void testSplitText_PreservesContent() {
        // 测试内容完整性
        String text = "人工智能技术正在快速发展。它在各个领域都有应用。未来将会更加智能化。" +
                "Natural language processing is important. Machine learning drives AI. " +
                "深度学习是关键技术！神经网络模拟大脑？";

        List<String> result = splitter.splitText(text);

        assertFalse(result.isEmpty(), "应该成功分割");

        // 验证内容没有丢失（通过检查关键词）
        String combined = String.join("", result);
        assertTrue(combined.contains("人工智能"), "应该包含原始内容");
        assertTrue(combined.contains("Machine learning"), "应该包含原始内容");
        assertTrue(combined.contains("深度学习"), "应该包含原始内容");
        printSplit(result);
    }

    @Test
    void testSplitText_OnlySpaces() {
        // 测试只有空格的文本（边界情况）
        String text = "word1 word2 word3 word4";
        List<String> result = splitter.splitText(text);

        assertFalse(result.isEmpty(), "应该处理只有空格分隔的文本");
    }

    @Test
    void testSplitText_VeryLongWithoutSeparators() {
        // 测试没有任何分隔符的超长文本（最坏情况）
        String text = "A".repeat(2500);

        List<String> result = splitter.splitText(text);

        assertTrue(result.size() > 1, "应该强制分割超长文本");

        for (String chunk : result) {
            assertTrue(chunk.length() <= 1000, "每个块不应超过1000字符");
        }
        printSplit(result);
    }

    private void printSplit(List<String> result) {
        for (String chunk : result) {
            System.out.println(chunk);
            System.out.println("==================");
        }
    }
}
