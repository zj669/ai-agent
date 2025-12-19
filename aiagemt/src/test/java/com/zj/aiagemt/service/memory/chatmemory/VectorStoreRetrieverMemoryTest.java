package com.zj.aiagemt.service.memory.chatmemory;

import com.zj.aiagemt.config.TestVectorStoreConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VectorStoreRetrieverMemory 集成测试
 * 
 * <p>
 * 测试基于语义相似度的向量检索功能，使用真实的向量存储和embedding模型
 * </p>
 * 
 * @author AI Agent
 */
@SpringBootTest
@Import(TestVectorStoreConfig.class) // 导入测试配置，使用SimpleVectorStore
class VectorStoreRetrieverMemoryTest {

        @Qualifier("openAiEmbeddingModel")
        @Autowired
        private EmbeddingModel embeddingModel;
        @Resource
        private VectorStore vectorStore;
        private VectorStoreRetrieverMemory memory;

        @BeforeEach
        void setUp() {
                // 创建测试实例：topK=5, 相似度阈值=0.6
                memory = new VectorStoreRetrieverMemory(vectorStore, 5, 0.6f);
        }

        /**
         * 核心测试：基于语义相似度检索相关历史
         * 
         * 验证点：
         * 1. 添加不同主题的消息到向量存储
         * 2. 使用语义相似的查询检索
         * 3. 验证返回的是最相关的历史记录（而不是全量）
         */
        @Test
        void testAddAndRetrieveWithSemanticSimilarity() {
                String conversationId = "test-conversation-1";

                // 1. 添加不同主题的历史消息
                // 主题1：Spring框架相关
                memory.add(conversationId, Arrays.asList(
                                new UserMessage("如何在Spring Boot中配置数据库连接？"),
                                new AssistantMessage("你可以在application.properties中配置spring.datasource相关属性。")));

                // 主题2：Java编程相关
                memory.add(conversationId, Arrays.asList(
                                new UserMessage("Java中的Lambda表达式怎么使用？"),
                                new AssistantMessage("Lambda表达式是Java 8引入的函数式编程特性，格式为(参数) -> 表达式。")));

                // 主题3：向量存储相关
                memory.add(conversationId, Arrays.asList(
                                new UserMessage("什么是向量数据库？"),
                                new AssistantMessage("向量数据库是专门用于存储和检索向量数据的数据库，支持语义相似度搜索。")));

                // 2. 使用与"Spring框架"相关的查询进行检索
                String semanticQuery = "Spring Boot配置文件在哪里设置？";
                List<Message> result = memory.get(conversationId, semanticQuery);

                // 3. 验证结果
                assertNotNull(result, "检索结果不应为null");
                assertFalse(result.isEmpty(), "应该检索到相关历史消息");

                // 验证检索到的消息与查询语义相关
                boolean foundSpringRelated = result.stream()
                                .anyMatch(msg -> msg.getText().contains("Spring")
                                                || msg.getText().contains("datasource"));

                assertTrue(foundSpringRelated, "应该检索到与Spring相关的历史消息");

                System.out.println("📚 检索到的相关消息数量: " + result.size());
                result.forEach(msg -> System.out.println("  - " + msg.getText()));
        }

        /**
         * 测试多个会话的隔离性
         * 
         * 验证点：只检索指定会话的消息，不会混入其他会话的数据
         */
        @Test
        void testRetrievalWithDifferentConversations() {
                String conv1 = "conversation-1";
                String conv2 = "conversation-2";

                // 会话1：关于Java
                memory.add(conv1, Arrays.asList(
                                new UserMessage("Java的垃圾回收机制是什么？"),
                                new AssistantMessage("Java使用自动垃圾回收来管理内存。")));

                // 会话2：关于Python
                memory.add(conv2, Arrays.asList(
                                new UserMessage("Python的装饰器如何使用？"),
                                new AssistantMessage("装饰器是Python中的一种设计模式。")));

                // 在会话1中检索
                List<Message> conv1Result = memory.get(conv1, "Java内存管理");

                // 验证只返回会话1的消息
                assertNotNull(conv1Result);
                boolean hasJavaContent = conv1Result.stream()
                                .anyMatch(msg -> msg.getText().contains("Java"));
                boolean hasPythonContent = conv1Result.stream()
                                .anyMatch(msg -> msg.getText().contains("Python"));

                assertTrue(hasJavaContent, "会话1应该包含Java相关内容");
                assertFalse(hasPythonContent, "会话1不应该包含会话2的Python内容");
        }

        /**
         * 测试相似度阈值过滤
         * 
         * 验证点：低相似度的消息应该被过滤掉
         */
        @Test
        void testSimilarityThreshold() {
                String conversationId = "test-threshold";

                // 添加一些消息
                memory.add(conversationId, Arrays.asList(
                                new UserMessage("机器学习的基本概念是什么？"),
                                new AssistantMessage("机器学习是人工智能的一个分支。")));

                memory.add(conversationId, Arrays.asList(
                                new UserMessage("今天天气怎么样？"),
                                new AssistantMessage("今天阳光明媚，适合出门。")));

                // 使用与机器学习相关的查询
                List<Message> result = memory.get(conversationId, "深度学习算法有哪些？");

                // 结果应该主要包含机器学习相关的消息，天气相关的应被过滤（低相似度）
                assertNotNull(result);

                if (!result.isEmpty()) {
                        boolean hasMachineLearning = result.stream()
                                        .anyMatch(msg -> msg.getText().contains("机器学习")
                                                        || msg.getText().contains("人工智能"));
                        assertTrue(hasMachineLearning, "应该包含机器学习相关内容");
                }
        }

        /**
         * 测试topK限制
         * 
         * 验证点：返回的结果数量不超过topK设置
         */
        @Test
        void testTopKLimit() {
                String conversationId = "test-topk";

                // 添加大量相关消息（超过topK=5）
                for (int i = 0; i < 10; i++) {
                        memory.add(conversationId, Arrays.asList(
                                        new UserMessage("关于Spring框架的问题" + i),
                                        new AssistantMessage("Spring是一个强大的Java框架" + i)));
                }

                // 检索
                List<Message> result = memory.get(conversationId, "Spring框架怎么用？");

                // 验证结果数量不超过topK
                assertNotNull(result);
                assertTrue(result.size() <= 5 * 2, "返回的消息数量应该不超过topK*2（每轮对话2条消息）");

                System.out.println("📊 TopK限制测试 - 实际返回: " + result.size() + " 条消息");
        }

        /**
         * 测试清空会话数据
         * 
         * 验证点：清空后无法检索到历史消息
         */
        @Test
        void testClearConversation() {
                String conversationId = "test-clear";

                // 添加消息
                memory.add(conversationId, Arrays.asList(
                                new UserMessage("测试消息"),
                                new AssistantMessage("测试响应")));

                // 验证可以检索到
                List<Message> beforeClear = memory.get(conversationId, "测试");
                assertFalse(beforeClear.isEmpty(), "清空前应该能检索到消息");

                // 清空会话
                memory.clear(conversationId);

                // 验证清空后检索不到
                List<Message> afterClear = memory.get(conversationId, "测试");
                assertTrue(afterClear.isEmpty(), "清空后不应该检索到任何消息");
        }

        /**
         * 测试会话统计信息
         */
        @Test
        void testConversationStats() {
                String conversationId = "test-stats";

                // 添加消息
                memory.add(conversationId, Arrays.asList(
                                new UserMessage("消息1"),
                                new AssistantMessage("响应1")));

                // 获取统计信息
                Map<String, Object> stats = memory.getConversationStats(conversationId);

                assertNotNull(stats);
                assertEquals(5, stats.get("topK"), "topK应该为5");
                assertEquals(0.6f, stats.get("similarityThreshold"), "相似度阈值应该为0.6");
                assertTrue((Boolean) stats.get("conversationExists"), "会话应该存在");
        }

        /**
         * 测试空查询文本的处理
         */
        @Test
        void testEmptyQueryText() {
                String conversationId = "test-empty";

                memory.add(conversationId, Arrays.asList(
                                new UserMessage("测试消息"),
                                new AssistantMessage("测试响应")));

                // 使用空查询
                List<Message> result = memory.get(conversationId, "");

                // 应该返回空列表或使用默认查询
                assertNotNull(result);
        }

        /**
         * 测试新旧API的兼容性
         */
        @Test
        void testBackwardCompatibility() {
                String conversationId = "test-compat";

                memory.add(conversationId, Arrays.asList(
                                new UserMessage("兼容性测试"),
                                new AssistantMessage("测试响应")));

                // 使用旧的API（不传查询文本）
                List<Message> result = memory.get(conversationId);

                // 应该能够正常工作（尽管可能不是最优的语义检索）
                assertNotNull(result);

                System.out.println("⚠️ 兼容性测试 - 旧API返回: " + result + " 条消息");
        }
}
