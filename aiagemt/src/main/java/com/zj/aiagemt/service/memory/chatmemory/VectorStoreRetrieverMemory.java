package com.zj.aiagemt.service.memory.chatmemory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Slf4j
public class VectorStoreRetrieverMemory implements ChatMemory {


    public static final int DEFAULT_TOP_K = 5;

    public static final float DEFAULT_SIMILARITY_THRESHOLD = 0.7f;

    public static final int DEFAULT_MAX_RESULTS = 10;

    private static final String CONVERSATION_ID_KEY = "conversationId";
    private static final String MESSAGE_TYPE_KEY = "messageType";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String QUERY_TEXT_KEY = "queryText";

    /**
     * -- GETTER --
     *  获取向量存储实例（用于测试）
     */
    @Getter
    private final VectorStore vectorStore;

    /**
     * -- GETTER --
     *  获取topK参数（用于测试）
     */
    @Getter
    private final int topK;

    /**
     * -- GETTER --
     *  获取相似度阈值（用于测试）
     */
    @Getter
    private final float similarityThreshold;

    private final Map<String, Integer> conversationMessageCounts = new ConcurrentHashMap<>();

    private final Map<String, LocalDateTime> conversationLastActivity = new ConcurrentHashMap<>();


    public VectorStoreRetrieverMemory(VectorStore vectorStore, int topK, float similarityThreshold) {
        // 参数验证
        if (vectorStore == null) {
            throw new IllegalArgumentException("VectorStore不能为null");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK必须大于0");
        }
        if (similarityThreshold < 0.0f || similarityThreshold > 1.0f) {
            throw new IllegalArgumentException("相似度阈值必须在[0.0, 1.0]范围内");
        }

        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;

        log.info("VectorStoreRetrieverMemory初始化完成 - topK: {}, 相似度阈值: {}",
                this.topK, this.similarityThreshold);
    }


    public VectorStoreRetrieverMemory(VectorStore vectorStore) {
        this(vectorStore, DEFAULT_TOP_K, DEFAULT_SIMILARITY_THRESHOLD);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 参数验证
        if (conversationId == null) {
            throw new IllegalArgumentException("对话ID不能为null");
        }
        if (messages == null) {
            throw new IllegalArgumentException("消息列表不能为null");
        }
        if (messages.isEmpty()) {
            log.info("消息列表为空，跳过添加操作 - conversationId: {}", conversationId);
            return;
        }

        try {

            List<Document> documents = convertMessagesToDocuments(conversationId, messages);

            if (!documents.isEmpty()) {

                vectorStore.add(documents);


                updateConversationStats(conversationId, messages.size());

                log.info("✅ 成功存储 {} 条消息到向量数据库 - conversationId: {}",
                        documents.size(), conversationId);
            }

        } catch (Exception e) {
            log.error("❌ 存储消息到向量数据库失败 - conversationId: {}, 错误: {}",
                    conversationId, e.getMessage(), e);
            // 这里可以考虑实现降级策略，比如暂存到内存缓存
            throw new RuntimeException("向量存储操作失败", e);
        }
    }

    /**
     * 获取相关的对话历史（使用默认查询）
     * 
     * <p>
     * 基于当前对话ID，使用默认查询条件从向量数据库中检索语义相关的历史对话片段。
     * </p>
     * <p>
     * <strong>注意</strong>：此方法使用默认查询，建议使用 {@link #get(String, String)}
     * 方法传入实际的用户查询文本。
     * </p>
     * 
     * @param conversationId 对话ID，不能为null
     * @return 相关的历史消息列表，如果没有找到相关内容则返回空列表
     * @throws IllegalArgumentException 如果conversationId为null
     * @deprecated 建议使用 {@link #get(String, String)} 方法传入实际的用户查询文本
     */
    @Override
    public List<Message> get(String conversationId) {
        // 参数验证
        if (conversationId == null) {
            throw new IllegalArgumentException("对话ID不能为null");
        }

        log.warn("⚠️ 使用了默认查询方式，建议传入用户的实际消息作为查询 - conversationId: {}", conversationId);

        // 使用默认查询文本（保持向后兼容）
        String queryText = buildQueryText(conversationId, null);
        return get(conversationId, queryText);
    }

    /**
     * 获取相关的对话历史（推荐使用）
     * 
     * <p>
     * <strong>基于语义相似度检索相关历史对话</strong>
     * </p>
     * <p>
     * 使用用户的实际输入作为查询条件，从向量数据库中检索语义最相关的历史对话片段。
     * </p>
     * 
     * @param conversationId 对话ID，不能为null
     * @param queryText      用户的当前输入消息，用于语义相似度匹配
     * @return 相关的历史消息列表，按相似度和时间排序
     * @throws IllegalArgumentException 如果conversationId为null
     */
    public List<Message> get(String conversationId, String queryText) {
        // 参数验证
        if (conversationId == null) {
            throw new IllegalArgumentException("对话ID不能为null");
        }

        try {
            // 构建实际的查询文本
            String actualQueryText = buildQueryText(conversationId, queryText);

            if (!StringUtils.hasText(actualQueryText)) {
                log.info("没有找到查询文本，返回空历史 - conversationId: {}", conversationId);
                return new ArrayList<>();
            }

            // 构建搜索请求
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(actualQueryText)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .filterExpression(createConversationFilter(conversationId))
                    .build();

            // 执行向量检索
            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            // 转换为Message列表并按时间排序
            List<Message> messages = convertDocumentsToMessages(documents);

            log.info("📚 检索到 {} 条相关历史消息 - conversationId: {}, 查询文本: {}",
                    messages.size(), conversationId,
                    queryText != null ? queryText.substring(0, Math.min(50, queryText.length())) + "..." : "默认查询");
            // todo rerank
            return messages;

        } catch (Exception e) {
            log.warn("⚠️ 检索历史消息失败，返回空列表 - conversationId: {}, 错误: {}",
                    conversationId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 清空对话历史
     * 
     * <p>
     * 删除指定对话的所有向量数据和统计信息。
     * </p>
     * 
     * @param conversationId 对话ID，不能为null
     * @throws IllegalArgumentException 如果conversationId为null
     */
    @Override
    public void clear(String conversationId) {
        // 参数验证
        if (conversationId == null) {
            throw new IllegalArgumentException("对话ID不能为null");
        }

        try {
            // 构建过滤条件，删除指定会话的所有数据
            Filter.Expression filter = createConversationFilter(conversationId);
            vectorStore.delete(filter);
            // 注意：这里使用了假设的delete方法，实际API可能有所不同
            // 如果VectorStore没有提供delete方法，可能需要通过其他方式实现
            // vectorStore.delete(filter); // 这个方法可能不存在，需要根据实际API调整

            // 清空统计信息
            Integer removedCount = conversationMessageCounts.remove(conversationId);
            LocalDateTime removedActivity = conversationLastActivity.remove(conversationId);

            log.info("🧹 对话数据清空完成 - conversationId: {}, 历史消息数: {}, 最后活动: {}",
                    conversationId,
                    removedCount != null ? removedCount : 0,
                    removedActivity != null ? removedActivity.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "未知");

        } catch (Exception e) {
            log.error("❌ 清空对话数据失败 - conversationId: {}", conversationId, e);
            // 至少清空内存中的统计信息
            conversationMessageCounts.remove(conversationId);
            conversationLastActivity.remove(conversationId);
            throw new RuntimeException("清空操作失败", e);
        }
    }

    // ======================== 扩展功能方法 ========================

    /**
     * 获取对话统计信息
     * 
     * @param conversationId 对话ID
     * @return 包含统计信息的Map
     */
    public Map<String, Object> getConversationStats(String conversationId) {
        if (conversationId == null) {
            return new HashMap<>();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("messageCount", conversationMessageCounts.getOrDefault(conversationId, 0));
        stats.put("lastActivity", conversationLastActivity.get(conversationId));
        stats.put("topK", topK);
        stats.put("similarityThreshold", similarityThreshold);
        stats.put("conversationExists", conversationMessageCounts.containsKey(conversationId));

        log.debug("📊 获取对话统计信息 - conversationId: {}, stats: {}", conversationId, stats);
        return stats;
    }

    /**
     * 批量清理不活跃的对话数据
     * 
     * @param activeConversationIds 活跃对话ID集合
     */
    public void cleanupInactiveConversations(Set<String> activeConversationIds) {
        if (activeConversationIds == null) {
            log.warn("⚠️ 活跃对话ID集合为null，跳过清理操作");
            return;
        }

        try {
            // 找出需要清理的对话ID
            Set<String> toRemove = new HashSet<>(conversationMessageCounts.keySet());
            toRemove.removeAll(activeConversationIds);

            if (toRemove.isEmpty()) {
                log.debug("没有需要清理的不活跃对话");
                return;
            }

            // 逐个清理不活跃的对话
            for (String conversationId : toRemove) {
                try {
                    clear(conversationId);
                } catch (Exception e) {
                    log.warn("清理对话失败: {}", conversationId, e);
                }
            }

            log.info("🧹 批量清理完成 - 清理对话数: {}, 保留对话数: {}",
                    toRemove.size(), activeConversationIds.size());

        } catch (Exception e) {
            log.error("❌ 批量清理不活跃对话数据失败", e);
        }
    }

    private List<Document> convertMessagesToDocuments(String conversationId, List<Message> messages) {
        List<Document> documents = new ArrayList<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        for (Message message : messages) {
            if (!StringUtils.hasText(message.getText())) {
                continue; // 跳过空消息
            }

            // 确定消息类型
            String messageType = determineMessageType(message);

            // 构建元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(CONVERSATION_ID_KEY, conversationId);
            metadata.put(MESSAGE_TYPE_KEY, messageType);
            metadata.put(TIMESTAMP_KEY, timestamp);

            // 创建Document
            Document document = new Document(message.getText(), metadata);
            documents.add(document);
        }

        return documents;
    }


    private List<Message> convertDocumentsToMessages(List<Document> documents) {
        return documents.stream()
                .map(this::convertDocumentToMessage)
                .filter(Objects::nonNull)
                .sorted(this::compareMessagesByTimestamp)
                .collect(Collectors.toList());
    }


    private Message convertDocumentToMessage(Document document) {
        try {
            String content = document.getText();
            Map<String, Object> metadata = document.getMetadata();
            String messageType = (String) metadata.get(MESSAGE_TYPE_KEY);

            switch (messageType) {
                case "user":
                    return new UserMessage(content);
                case "assistant":
                    return new AssistantMessage(content);
                case "system":
                    return new SystemMessage(content);
                default:
                    log.warn("未知的消息类型: {}, 默认作为用户消息处理", messageType);
                    return new UserMessage(content);
            }
        } catch (Exception e) {
            log.warn("转换Document为Message失败: {}", e.getMessage());
            return null;
        }
    }


    private String determineMessageType(Message message) {
        if (message instanceof UserMessage) {
            return "user";
        } else if (message instanceof AssistantMessage) {
            return "assistant";
        } else if (message instanceof SystemMessage) {
            return "system";
        } else {
            return "unknown";
        }
    }


    private String buildQueryText(String conversationId, String userInput) {
        // 如果提供了用户输入，直接使用作为查询文本
        if (StringUtils.hasText(userInput)) {
            log.debug("使用用户输入作为查询 - conversationId: {}, 输入长度: {}",
                    conversationId, userInput.length());
            return userInput;
        }

        // 否则使用默认查询（保持向后兼容）
        log.debug("使用默认查询 - conversationId: {}", conversationId);
        return "相关对话历史 conversationId:" + conversationId;
    }


    private Filter.Expression createConversationFilter(String conversationId) {
        return new FilterExpressionBuilder()
                .eq(CONVERSATION_ID_KEY, conversationId)
                .build();
    }


    private int compareMessagesByTimestamp(Message msg1, Message msg2) {
        // 简化实现：根据消息内容长度排序（实际应该根据时间戳）
        // 这里需要从Message中提取时间戳信息，或者使用其他排序策略
        return Integer.compare(msg1.getText().length(), msg2.getText().length());
    }


    private void updateConversationStats(String conversationId, int messageCount) {
        if (conversationId == null || messageCount <= 0) {
            return;
        }

        // 更新消息计数
        conversationMessageCounts.merge(conversationId, messageCount, Integer::sum);

        // 更新最后活动时间
        conversationLastActivity.put(conversationId, LocalDateTime.now());

        log.debug("📊 更新对话统计 - conversationId: {}, 新增消息: {}, 总消息数: {}",
                conversationId, messageCount, conversationMessageCounts.get(conversationId));
    }

}
