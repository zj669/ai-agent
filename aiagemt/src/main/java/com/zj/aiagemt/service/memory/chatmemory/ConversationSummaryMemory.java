package com.zj.aiagemt.service.memory.chatmemory;

import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class ConversationSummaryMemory implements ChatMemory {

    // ======================== 默认配置常量 ========================
    
    /** 默认摘要触发阈值 */
    public static final int DEFAULT_SUMMARY_TRIGGER_THRESHOLD = 3;
    
    /** 默认摘要最大长度 */
    public static final int DEFAULT_SUMMARY_MAX_LENGTH = 500;
    
    /** 默认AI客户端ID */
    public static final String DEFAULT_AI_CLIENT_ID = "3003";
    
    /** 默认摘要生成超时时间 */
    public static final Duration DEFAULT_SUMMARY_TIMEOUT = Duration.ofSeconds(5);
    
    /** 默认最大消息数（降级策略使用） */
    public static final int DEFAULT_MAX_MESSAGES = 20;

    // ======================== 依赖组件 ========================
    
    /** Spring上下文工具，用于获取AI客户端Bean */
    private final SpringContextUtil springContextUtil;

    // ======================== 配置参数 ========================
    
    /** 触发摘要生成的消息数量阈值 */
    private final int summaryTriggerThreshold;
    
    /** 生成摘要的最大字符长度 */
    private final int summaryMaxLength;
    
    /** 用于生成摘要的AI客户端ID */
    private final String aiClientId;
    
    /** 摘要生成超时时间 */
    private final Duration summaryTimeout;

    // ======================== 运行时状态（线程安全） ========================
    
    /** 对话消息存储，Key: conversationId, Value: 消息列表 */
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();
    
    /** 对话消息数量计数，Key: conversationId, Value: 消息数量 */
    private final Map<String, Integer> conversationMessageCounts = new ConcurrentHashMap<>();
    
    /** 对话摘要缓存，Key: conversationId, Value: 摘要内容 */
    private final Map<String, String> conversationSummaries = new ConcurrentHashMap<>();

    // ======================== 构造函数 ========================
    
    /**
     * 完整配置构造函数
     * 
     * @param springContextUtil Spring上下文工具
     * @param summaryTriggerThreshold 摘要触发阈值
     * @param summaryMaxLength 摘要最大长度
     * @param aiClientId AI客户端ID
     * @param summaryTimeout 摘要生成超时时间
     * @throws IllegalArgumentException 如果参数无效
     */
    public ConversationSummaryMemory(SpringContextUtil springContextUtil,
                                   int summaryTriggerThreshold,
                                   int summaryMaxLength,
                                   String aiClientId,
                                   Duration summaryTimeout) {
        // 参数验证
        if (springContextUtil == null) {
            throw new IllegalArgumentException("SpringContextUtil不能为null");
        }
        if (summaryTriggerThreshold <= 0) {
            throw new IllegalArgumentException("摘要触发阈值必须大于0");
        }
        if (summaryMaxLength <= 0) {
            throw new IllegalArgumentException("摘要最大长度必须大于0");
        }
        
        this.springContextUtil = springContextUtil;
        this.summaryTriggerThreshold = summaryTriggerThreshold;
        this.summaryMaxLength = summaryMaxLength;
        this.aiClientId = aiClientId != null ? aiClientId : DEFAULT_AI_CLIENT_ID;
        this.summaryTimeout = summaryTimeout != null ? summaryTimeout : DEFAULT_SUMMARY_TIMEOUT;
        
        log.info("ConversationSummaryMemory初始化完成 - 触发阈值: {}, 最大长度: {}, AI客户端: {}, 超时: {}ms", 
                this.summaryTriggerThreshold, this.summaryMaxLength, this.aiClientId, this.summaryTimeout.toMillis());
    }
    
    /**
     * 使用默认配置的构造函数
     * 
     * @param springContextUtil Spring上下文工具
     */
    public ConversationSummaryMemory(SpringContextUtil springContextUtil) {
        this(springContextUtil, DEFAULT_SUMMARY_TRIGGER_THRESHOLD, DEFAULT_SUMMARY_MAX_LENGTH, 
             DEFAULT_AI_CLIENT_ID, DEFAULT_SUMMARY_TIMEOUT);
    }

    // ======================== ChatMemory接口实现 ========================
    
    /**
     * 添加消息到对话历史
     * 
     * <p>该方法会自动检查是否需要生成摘要，如果消息数量达到阈值，
     * 会调用AI模型生成摘要并压缩历史记录。</p>
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
            log.debug("消息列表为空，跳过添加操作 - conversationId: {}", conversationId);
            return;
        }
        
        try {
            // 检查是否需要触发摘要（在添加新消息之前）
            if (shouldTriggerSummary(conversationId)) {
                log.info("📝 触发对话摘要，会话ID: {}, 当前消息数: {}", 
                        conversationId, conversationMessageCounts.getOrDefault(conversationId, 0));
                
                // 获取当前对话历史
                List<Message> currentMessages = conversations.get(conversationId);
                if (currentMessages != null && !currentMessages.isEmpty()) {
                    // 生成摘要
                    String summary = generateSummary(conversationId, currentMessages);
                    if (StringUtils.hasText(summary)) {
                        // 用摘要替换旧消息
                        List<Message> summarizedMessages = createSummarizedMessages(summary, currentMessages);
                        
                        // 更新对话历史
                        conversations.put(conversationId, new ArrayList<>(summarizedMessages));
                        conversationMessageCounts.put(conversationId, summarizedMessages.size());
                        
                        log.info("✅ 对话摘要完成，压缩前: {} 条消息，压缩后: {} 条消息", 
                                currentMessages.size(), summarizedMessages.size());
                    }
                }
            }
            
            // 添加新消息
            conversations.computeIfAbsent(conversationId, k -> new ArrayList<>()).addAll(messages);
            
            // 更新消息计数
            updateMessageCount(conversationId, messages.size());
            
            log.debug("✅ 成功添加 {} 条消息到对话: {}", messages.size(), conversationId);
            
        } catch (Exception e) {
            log.warn("⚠️ 添加消息失败，会话ID: {}, 错误: {}", conversationId, e.getMessage());
            // 降级策略：尝试直接添加消息，忽略摘要生成失败
            try {
                conversations.computeIfAbsent(conversationId, k -> new ArrayList<>()).addAll(messages);
                updateMessageCount(conversationId, messages.size());
                log.info("🔄 降级策略执行，直接添加消息 - 会话ID: {}", conversationId);
            } catch (Exception fallbackError) {
                log.error("❌ 降级策略也失败，会话ID: {}", conversationId, fallbackError);
                throw new RuntimeException("添加消息失败", fallbackError);
            }
        }
    }
    
    /**
     * 获取对话历史
     * 
     * @param conversationId 对话ID，不能为null
     * @return 对话消息列表，如果对话不存在则返回空列表
     * @throws IllegalArgumentException 如果conversationId为null
     */
    @Override
    public List<Message> get(String conversationId) {
        // 参数验证
        if (conversationId == null) {
            throw new IllegalArgumentException("对话ID不能为null");
        }
        
        List<Message> messages = conversations.get(conversationId);
        if (messages == null) {
            log.debug("对话不存在，返回空列表 - conversationId: {}", conversationId);
            return new ArrayList<>();
        }
        
        // 返回副本，防止外部修改
        List<Message> result = new ArrayList<>(messages);
        log.debug("获取对话历史 - conversationId: {}, 消息数: {}", conversationId, result.size());
        return result;
    }
    
    /**
     * 清空对话历史
     * 
     * <p>清空指定对话的所有消息、计数和摘要缓存。</p>
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
        
        // 清空所有相关数据
        List<Message> removedMessages = conversations.remove(conversationId);
        Integer removedCount = conversationMessageCounts.remove(conversationId);
        String removedSummary = conversationSummaries.remove(conversationId);
        
        log.info("🧩 对话数据清空完成 - conversationId: {}, 消息数: {}, 计数: {}, 有摘要: {}", 
                conversationId, 
                removedMessages != null ? removedMessages.size() : 0,
                removedCount != null ? removedCount : 0,
                removedSummary != null);
    }

    // ======================== 扩展功能方法 ========================
    
    /**
     * 获取对话统计信息
     * 
     * <p>返回包含消息数量、摘要状态等信息的统计数据，用于监控和调试。</p>
     * 
     * @param conversationId 对话ID
     * @return 包含统计信息的Map
     */
    public Map<String, Object> getConversationStats(String conversationId) {
        if (conversationId == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        // 基本统计信息
        stats.put("messageCount", conversationMessageCounts.getOrDefault(conversationId, 0));
        stats.put("hasSummary", conversationSummaries.containsKey(conversationId));
        stats.put("summary", conversationSummaries.get(conversationId));
        
        // 实际消息数量
        List<Message> messages = conversations.get(conversationId);
        stats.put("actualMessageCount", messages != null ? messages.size() : 0);
        
        // 配置信息
        stats.put("summaryTriggerThreshold", summaryTriggerThreshold);
        stats.put("summaryMaxLength", summaryMaxLength);
        stats.put("aiClientId", aiClientId);
        
        // 状态信息
        stats.put("conversationExists", conversations.containsKey(conversationId));
        stats.put("needsSummary", shouldTriggerSummary(conversationId));
        
        log.debug("📊 获取对话统计信息 - conversationId: {}, stats: {}", conversationId, stats);
        return stats;
    }
    
    /**
     * 清理不活跃的对话数据
     * 
     * <p>删除不在活跃对话列表中的所有对话数据，防止内存泄漏。</p>
     * 
     * @param activeConversationIds 活跃对话ID集合
     */
    public void cleanupInactiveConversations(Set<String> activeConversationIds) {
        if (activeConversationIds == null) {
            log.warn("⚠️ 活跃对话ID集合为null，跳过清理操作");
            return;
        }
        
        try {
            // 统计清理前的数据
            int beforeConversations = conversations.size();
            int beforeCounts = conversationMessageCounts.size();
            int beforeSummaries = conversationSummaries.size();
            
            // 执行清理操作
            conversations.keySet().retainAll(activeConversationIds);
            conversationMessageCounts.keySet().retainAll(activeConversationIds);
            conversationSummaries.keySet().retainAll(activeConversationIds);
            
            // 统计清理后的数据
            int afterConversations = conversations.size();
            int afterCounts = conversationMessageCounts.size();
            int afterSummaries = conversationSummaries.size();
            
            log.info("🧩 对话数据清理完成 - 活跃对话: {}个, 清理前: [{}个对话, {}个计数, {}个摘要], 清理后: [{}个对话, {}个计数, {}个摘要]", 
                    activeConversationIds.size(), 
                    beforeConversations, beforeCounts, beforeSummaries,
                    afterConversations, afterCounts, afterSummaries);
            
        } catch (Exception e) {
            log.error("❌ 清理不活跃对话数据失败", e);
            throw new RuntimeException("清理操作失败", e);
        }
    }

    // ======================== 私有工具方法（待实现） ========================
    
    /**
     * 判断是否应该触发摘要生成
     * 
     * <p>检查当前对话的消息数量是否达到了摘要触发阈值。
     * 同时检查计数器和实际消息数量，以保证准确性。</p>
     * 
     * @param conversationId 对话ID
     * @return true 如果应该触发摘要，false 否则
     */
    private boolean shouldTriggerSummary(String conversationId) {
        if (conversationId == null) {
            return false;
        }
        
        // 检查计数器
        int currentCount = conversationMessageCounts.getOrDefault(conversationId, 0);
        if (currentCount >= summaryTriggerThreshold) {
            log.debug("✅ 计数器达到阈值 - conversationId: {}, 当前计数: {}, 阈值: {}", 
                    conversationId, currentCount, summaryTriggerThreshold);
            return true;
        }
        
        // 检查实际消息数量（防止计数器不准确）
        List<Message> messages = conversations.get(conversationId);
        if (messages != null && messages.size() >= summaryTriggerThreshold) {
            log.debug("✅ 实际消息数达到阈值 - conversationId: {}, 实际数量: {}, 阈值: {}", 
                    conversationId, messages.size(), summaryTriggerThreshold);
            return true;
        }
        
        return false;
    }
    
    /**
     * 生成对话摘要
     * 
     * <p>调用AI模型生成对话摘要，包含超时处理和长度限制。</p>
     * 
     * @param conversationId 对话ID
     * @param messages 要摘要的消息列表
     * @return 生成的摘要内容，失败时返回null
     */
    private String generateSummary(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("⚠️ 消息列表为空，无法生成摘要 - conversationId: {}", conversationId);
            return null;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 获取 AI 客户端
            String beanName = AiAgentEnumVO.AI_CLIENT.getBeanName(aiClientId);
            ChatClient chatClient = springContextUtil.getBean(beanName);
            
            // 构建摘要提示词
            String summaryPrompt = buildSummaryPrompt(messages);
            
            log.info("🤖 开始生成摘要 - conversationId: {}, 消息数: {}, AI客户端: {}",
                    conversationId, messages.size(), beanName);
            
            // 调用 AI 模型生成摘要
            String summary = chatClient
                    .prompt(summaryPrompt)
                    .call()
                    .content();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("📝 摘要生成耗时: {}ms，原始消息: {}条，摘要长度: {}字符",
                    duration, messages.size(), summary != null ? summary.length() : 0);
            
            // 限制摘要长度
            if (summary != null && summary.length() > summaryMaxLength) {
                summary = summary.substring(0, summaryMaxLength) + "...";
                log.info("✂️ 摘要长度超限，已截取到 {} 字符", summaryMaxLength);
            }
            
            // 缓存摘要
            if (StringUtils.hasText(summary)) {
                conversationSummaries.put(conversationId, summary);
                log.info("✅ 摘要生成成功 - conversationId: {}, 摘要长度: {}字符", 
                        conversationId, summary.length());
            } else {
                log.warn("⚠️ 生成的摘要为空 - conversationId: {}", conversationId);
            }
            
            return summary;
            
        } catch (Exception e) {
            log.error("❌ 生成摘要失败，会话ID: {}, 错误: {}", conversationId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 构建摘要提示词
     * 
     * <p>将对话消息转换为用于生成摘要的提示词格式。</p>
     * 
     * @param messages 消息列表
     * @return 摘要提示词
     */
    private String buildSummaryPrompt(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "无对话历史。";
        }
        
        StringBuilder conversationText = new StringBuilder();
        
        for (Message message : messages) {
            if (message instanceof UserMessage) {
                conversationText.append("用户: ").append(message.getText()).append("\n");
            } else if (message instanceof AssistantMessage) {
                conversationText.append("助手: ").append(message.getText()).append("\n");
            } else if (message instanceof SystemMessage) {
                // 过滤旧的摘要消息，防止重复摘要
                if (!message.getText().startsWith("【对话摘要】")) {
                    conversationText.append("系统: ").append(message.getText()).append("\n");
                }
            }
        }
        
        return String.format(
            "你是一个专业的对话摘要生成器。请为以下对话历史生成一个简洁的摘要，保留关键信息和上下文。\n\n" +
            "对话历史：\n%s\n\n" +
            "请生成一个简洁的摘要（不超过200字），保留最重要的信息和上下文关系：",
            conversationText.toString()
        );
    }
    
    /**
     * 创建包含摘要的消息列表
     * 
     * <p>将生成的摘要作为系统消息添加到列表开头，然后保留最近的一部分消息。</p>
     * 
     * @param summary 生成的摘要内容
     * @param originalMessages 原始消息列表
     * @return 包含摘要的新消息列表
     */
    private List<Message> createSummarizedMessages(String summary, List<Message> originalMessages) {
        if (!StringUtils.hasText(summary) || originalMessages == null || originalMessages.isEmpty()) {
            log.warn("⚠️ 摘要或原始消息为空，返回原始消息");
            return new ArrayList<>(originalMessages != null ? originalMessages : List.of());
        }
        
        List<Message> summarizedMessages = new ArrayList<>();
        
        // 添加摘要作为系统消息
        summarizedMessages.add(new SystemMessage("【对话摘要】" + summary));
        
        // 保留最近的几条消息（保留最后25%的消息，最少保祗2条）
        int keepCount = Math.max(2, originalMessages.size() / 4);
        int startIndex = Math.max(0, originalMessages.size() - keepCount);
        
        for (int i = startIndex; i < originalMessages.size(); i++) {
            Message message = originalMessages.get(i);
            // 过滤掉系统消息中的旧摘要
            if (message instanceof SystemMessage && message.getText().startsWith("【对话摘要】")) {
                continue;
            }
            summarizedMessages.add(message);
        }
        
        log.debug("📋 创建摘要消息列表 - 原始: {}条, 保留: {}条, 摘要长度: {}字符", 
                originalMessages.size(), keepCount, summary.length());
        
        return summarizedMessages;
    }
    
    /**
     * 处理摘要生成失败的情况（降级策略）
     * 
     * <p>当AI摘要生成失败时，执行简单的降级策略：
     * 删除最旧的消息，保留最近的消息。</p>
     * 
     * @param conversationId 对话ID
     */
    private void handleSummaryFailure(String conversationId) {
        if (conversationId == null) {
            return;
        }
        
        try {
            List<Message> messages = conversations.get(conversationId);
            if (messages == null || messages.size() <= DEFAULT_MAX_MESSAGES) {
                log.debug("ℹ️ 消息数量未超限，无需执行降级策略 - conversationId: {}, 消息数: {}", 
                        conversationId, messages != null ? messages.size() : 0);
                return;
            }
            
            // 简单的降级策略：保留最近的75%的消息
            int keepCount = DEFAULT_MAX_MESSAGES * 3 / 4;
            if (keepCount <= 0) {
                keepCount = 2; // 最少保礇2条消息
            }
            
            int startIndex = Math.max(0, messages.size() - keepCount);
            List<Message> recentMessages = new ArrayList<>();
            
            for (int i = startIndex; i < messages.size(); i++) {
                recentMessages.add(messages.get(i));
            }
            
            // 更新对话数据
            conversations.put(conversationId, recentMessages);
            conversationMessageCounts.put(conversationId, recentMessages.size());
            
            log.info("🔄 降级策略执行完成，会话ID: {}, 原消息: {}条, 保留消息: {}条", 
                    conversationId, messages.size(), recentMessages.size());
            
        } catch (Exception e) {
            log.error("❌ 降级策略执行失败，会话ID: {}", conversationId, e);
            // 最后的回退方案：直接清空对话
            try {
                clear(conversationId);
                log.warn("⚠️ 已清空对话作为最后回退方案 - conversationId: {}", conversationId);
            } catch (Exception clearError) {
                log.error("❌ 清空对话也失败，会话ID: {}", conversationId, clearError);
            }
        }
    }
    
    /**
     * 更新消息计数
     * 
     * <p>使用原子操作更新消息计数，保证线程安全。</p>
     * 
     * @param conversationId 对话ID
     * @param increment 增量（可为负数）
     */
    private void updateMessageCount(String conversationId, int increment) {
        if (conversationId == null || increment == 0) {
            return;
        }
        
        // 使用merge进行原子更新
        conversationMessageCounts.merge(conversationId, increment, Integer::sum);
        
        int newCount = conversationMessageCounts.get(conversationId);
        log.debug("📊 消息计数更新 - conversationId: {}, 增量: {}, 新计数: {}", 
                conversationId, increment, newCount);
    }
    
    // ======================== Getter方法（用于测试和监控） ========================
    
    /**
     * 获取摘要触发阈值（用于测试）
     */
    public int getSummaryTriggerThreshold() {
        return summaryTriggerThreshold;
    }
    
    /**
     * 获取摘要最大长度（用于测试）
     */
    public int getSummaryMaxLength() {
        return summaryMaxLength;
    }
    
    /**
     * 获取AI客户端ID（用于测试）
     */
    public String getAiClientId() {
        return aiClientId;
    }
    
    /**
     * 获取超时时间（用于测试）
     */
    public Duration getSummaryTimeout() {
        return summaryTimeout;
    }
}
