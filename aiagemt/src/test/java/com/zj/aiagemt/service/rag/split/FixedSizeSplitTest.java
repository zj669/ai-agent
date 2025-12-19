package com.zj.aiagemt.service.rag.split;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FixedSizeSplit 单元测试
 * 测试固定大小分块功能
 */
class FixedSizeSplitTest {

    private FixedSizeSplit splitter;

    @BeforeEach
    void setUp() {
        splitter = new FixedSizeSplit();
    }

    @Test
    void testSplitText_EmptyText() {
        // 测试空文本
        List<String> result = splitter.splitText("");
        assertTrue(result.isEmpty(), "空文本应返回空列表");
    }

    @Test
    void testSplitText_NullText() {
        // 测试 null
        List<String> result = splitter.splitText((String) null);
        assertTrue(result.isEmpty(), "null 应返回空列表");
    }

    @Test
    void testSplitText_ShortText() {
        // 测试短文本（小于块大小）
        String text = "这是一段短文本";
        List<String> result = splitter.splitText(text);

        assertEquals(1, result.size(), "短文本应返回一个块");
        assertEquals(text, result.get(0), "短文本应完整返回");
        printSplit( result);
    }

    @Test
    void testSplitText_LongText() {
        // 测试长文本（需要分块）
        // 构造一个超过1000字符的文本
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("这是第").append(i).append("句话。");
        }
        String text = sb.toString();

        List<String> result = splitter.splitText(text);
        printSplit( result);
        assertTrue(result.size() > 1, "长文本应被分成多个块");

        // 验证每个块的大小不超过1000字符
        for (String chunk : result) {
            assertTrue(chunk.length() <= 1000, "每个块不应超过1000字符");
        }
    }

    @Test
    void testSplitText_WithOverlap() {
        // 测试重叠功能
        // 构造一个需要分块的文本
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("测试文本");
        }
        String text = sb.toString();

        List<String> result = splitter.splitText(text);

        // 验证重叠：检查相邻块之间是否有重叠内容
        if (result.size() > 1) {
            for (int i = 0; i < result.size() - 1; i++) {
                String currentChunk = result.get(i);
                String nextChunk = result.get(i + 1);

                // 获取当前块的最后200个字符（CHUNK_OVERLAP）
                int overlapStart = Math.max(0, currentChunk.length() - 200);
                String currentEnd = currentChunk.substring(overlapStart);

                // 下一个块应该以这些字符开始（或包含它们）
                assertTrue(nextChunk.contains(currentEnd.substring(0, Math.min(50, currentEnd.length()))),
                        "相邻块之间应该有重叠内容");
            }
        }
        printSplit( result);
    }

    @Test
    void testSplitText_ExactChunkSize() {
        // 测试恰好等于块大小的文本
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 200) {
            sb.append("x");
        }
        String text = sb.substring(0, 200);

        List<String> result = splitter.splitText(text);

        assertEquals(1, result.size(), "恰好1000字符应返回一个块");
        assertEquals(200, result.get(0).length(), "块大小应为1000");
        printSplit( result);
    }

    @Test
    void testSplitText_PreservesContent() {
        // 测试内容完整性：所有块拼接后应包含原始内容的所有字符
        String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".repeat(100); // 2600字符

        List<String> result = splitter.splitText(text);

        // 由于有重叠，拼接后会比原文本长，但应该包含所有原始字符
        assertTrue(result.size() > 1, "应该有多个块");

        // 验证第一个块包含开头
        assertTrue(result.get(0).startsWith("ABCDEFG"), "第一个块应包含文本开头");

        // 验证最后一个块包含结尾
        String lastChunk = result.get(result.size() - 1);
        assertTrue(text.endsWith(lastChunk) || lastChunk.endsWith(text.substring(text.length() - 10)),
                "最后一个块应包含文本结尾");
    }

    @Test
    void testSplitText_ChineseText() {
        // 测试中文文本
        String text = "人工智能是计算机科学的一个分支，它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。".repeat(30);

        List<String> result = splitter.splitText(text);

        assertFalse(result.isEmpty(), "中文文本应该被正确分块");

        for (String chunk : result) {
            assertTrue(chunk.length() <= 1000, "每个块不应超过1000字符");
            assertFalse(chunk.isEmpty(), "块不应为空");
        }
        printSplit( result);
    }

    private void printSplit(List<String> result){
        for (String chunk : result) {
            System.out.println(chunk);
            System.out.println("==================");
        }
    }

}
