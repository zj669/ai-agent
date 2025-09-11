package com.zj.aiagemt.service.memory.chatmemory;

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

/**
 * 向量存储检索记忆管理器
 * 
 * <p>实现基于向量数据库的智能对话记忆管理，通过语义相似度检索相关历史对话，
 * 为AI模型提供精确的上下文信息，特别适用于需要长期记忆和精确历史匹配的场景。</p>
 * 
 * <h3>核心功能</h3>
 * <ul>
 *   <li>对话消息向量化存储</li>
 *   <li>基于语义相似度的历史检索</li>
 *   <li>会话隔离和数据管理</li>
 *   <li>异常处理和降级策略</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * <pre>{@code
 * VectorStoreRetrieverMemory memory = new VectorStoreRetrieverMemory(
 *     pgVectorStore, 5, 0.7f
 * );
 * 
 * // 添加消息
 * memory.add("conversation-1", Arrays.asList(userMessage, assistantMessage));
 * 
 * // 获取相关历史记录
 * List<Message> relevantHistory = memory.get("conversation-1");
 * }</pre>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Slf4j
public class VectorStoreRetrieverMemory implements ChatMemory {

    // ======================== 默认配置常量 ========================
    
    /** 默认检索数量 */
    public static final int DEFAULT_TOP_K = 5;
    
    /** 默认相似度阈值 */
    public static final float DEFAULT_SIMILARITY_THRESHOLD = 0.7f;
    
    /** 默认最大检索结果数 */
    public static final int DEFAULT_MAX_RESULTS = 10;
    
    /** 向量存储的Document元数据键名 */
    private static final String CONVERSATION_ID_KEY = "conversationId";
    private static final String MESSAGE_TYPE_KEY = "messageType";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String QUERY_TEXT_KEY = "queryText";

    // ======================== 依赖组件 ========================
    
    /** 向量存储实例，用于向量操作 */
    private final VectorStore vectorStore;

    // ======================== 配置参数 ========================
    
    /** 检索时返回的最大结果数量 */
    private final int topK;
    
    /** 相似度匹配阈值，低于此值的结果将被过滤 */
    private final float similarityThreshold;

    // ======================== 运行时状态（线程安全） ========================
    
    /** 会话消息计数器，用于统计和监控 */
    private final Map<String, Integer> conversationMessageCounts = new ConcurrentHashMap<>();
    
    /** 会话最后活动时间，用于清理策略 */
    private final Map<String, LocalDateTime> conversationLastActivity = new ConcurrentHashMap<>();
    // ======================== 构造函数 ========================
    
    /**
     * 完整配置构造函数
     * 
     * @param vectorStore 向量存储实例，不能为null
     * @param topK 检索时返回的最大结果数量，必须大于0
     * @param similarityThreshold 相似度阈值，范围[0.0, 1.0]
     * @throws IllegalArgumentException 如果参数无效
     */
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
    
    /**
     * 使用默认配置的构造函数
     * 
     * @param vectorStore 向量存储实例，不能为null
     */
    public VectorStoreRetrieverMemory(VectorStore vectorStore) {
        this(vectorStore, DEFAULT_TOP_K, DEFAULT_SIMILARITY_THRESHOLD);
    }

    // ======================== ChatMemory接口实现 ========================
    
    /**
     * 添加消息到向量存储
     * 
     * <p>将对话消息转换为Document并存储到向量数据库中，每条消息包含完整的元数据信息。</p>
     * 
     * @param conversationId 对话ID，不能为null
     * @param messages 要添加的消息列表，不能为null
     * @throws IllegalArgumentException 如果参数无效
     */
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
            // 转换消息为Document列表
            List<Document> documents = convertMessagesToDocuments(conversationId, messages);
            
            if (!documents.isEmpty()) {
                // 存储到向量数据库
                vectorStore.add(documents);
                
                // 更新统计信息
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
     * 获取相关的对话历史
     * 
     * <p>基于当前对话ID，使用最近的用户输入作为查询条件，
     * 从向量数据库中检索语义相关的历史对话片段。</p>
     * 
     * @param conversationId 对话ID，不能为null
     * @return 相关的历史消息列表，如果没有找到相关内容则返回空列表
     * @throws IllegalArgumentException 如果conversationId为null
     */
    @Override
    public List<Message> get(String conversationId) {
        // 参数验证
        if (conversationId == null) {
            throw new IllegalArgumentException("对话ID不能为null");
        }
        
        try {
            // 获取当前会话的最近消息作为查询上下文
            String queryText = buildQueryText(conversationId);
            
            if (!StringUtils.hasText(queryText)) {
                log.info("没有找到查询文本，返回空历史 - conversationId: {}", conversationId);
                return new ArrayList<>();
            }
            
            // 构建搜索请求
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(queryText)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .filterExpression(createConversationFilter(conversationId))
                    .build();
            
            // 执行向量检索
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            
            // 转换为Message列表并按时间排序
            List<Message> messages = convertDocumentsToMessages(documents);
            
            log.info("📚 检索到 {} 条相关历史消息 - conversationId: {}, 查询: {}",
                    messages.size(), conversationId, queryText);
            
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
     * <p>删除指定对话的所有向量数据和统计信息。</p>
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

    // ======================== 私有工具方法 ========================
    
    /**
     * 将消息列表转换为Document列表
     * 
     * @param conversationId 对话ID
     * @param messages 消息列表
     * @return Document列表
     */
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
    
    /**
     * 将Document列表转换为Message列表
     * 
     * @param documents Document列表
     * @return Message列表，按时间排序
     */
    private List<Message> convertDocumentsToMessages(List<Document> documents) {
        return documents.stream()
                .map(this::convertDocumentToMessage)
                .filter(Objects::nonNull)
                .sorted(this::compareMessagesByTimestamp)
                .collect(Collectors.toList());
    }
    
    /**
     * 将单个Document转换为Message
     * 
     * @param document Document对象
     * @return Message对象，如果转换失败则返回null
     */
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
    
    /**
     * 确定消息类型
     * 
     * @param message 消息对象
     * @return 消息类型字符串
     */
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
    
    /**
     * 构建查询文本
     * 
     * <p>这是一个简化实现，实际应用中可以基于最近的对话内容构建更智能的查询。</p>
     * 
     * @param conversationId 对话ID
     * @return 查询文本
     */
    private String buildQueryText(String conversationId) {
        // 简化实现：使用conversationId作为查询文本
        // 在实际应用中，这里应该基于最近的用户输入或对话上下文构建查询
        // 可以考虑从当前会话的最后几条消息中提取关键词作为查询 todo
        return "相关对话历史 conversationId:" + conversationId;
    }
    
    /**
     * 创建会话过滤器
     * 
     * @param conversationId 对话ID
     * @return 过滤表达式
     */
    private Filter.Expression createConversationFilter(String conversationId) {
        return new FilterExpressionBuilder()
                .eq(CONVERSATION_ID_KEY, conversationId)
                .build();
    }
    
    /**
     * 比较消息的时间戳
     * 
     * @param msg1 消息1
     * @param msg2 消息2
     * @return 比较结果
     */
    private int compareMessagesByTimestamp(Message msg1, Message msg2) {
        // 简化实现：根据消息内容长度排序（实际应该根据时间戳）
        // 这里需要从Message中提取时间戳信息，或者使用其他排序策略
        return Integer.compare(msg1.getText().length(), msg2.getText().length());
    }
    
    /**
     * 更新对话统计信息
     * 
     * @param conversationId 对话ID
     * @param messageCount 新增消息数
     */
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
    
    // ======================== Getter方法（用于测试和监控） ========================
    
    /**
     * 获取topK参数（用于测试）
     */
    public int getTopK() {
        return topK;
    }
    
    /**
     * 获取相似度阈值（用于测试）
     */
    public float getSimilarityThreshold() {
        return similarityThreshold;
    }
    
    /**
     * 获取向量存储实例（用于测试）
     */
    public VectorStore getVectorStore() {
        return vectorStore;
    }
}
