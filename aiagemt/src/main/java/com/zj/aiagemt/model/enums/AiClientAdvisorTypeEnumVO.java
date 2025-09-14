package com.zj.aiagemt.model.enums;


import com.zj.aiagemt.model.vo.AiClientAdvisorVO;
import com.zj.aiagemt.service.memory.ConversationSummaryMemoryAdvisor;
import com.zj.aiagemt.service.memory.VectorStoreRetrieverMemoryAdvisor;
import com.zj.aiagemt.service.memory.chatmemory.ConversationSummaryMemory;
import com.zj.aiagemt.service.memory.chatmemory.VectorStoreRetrieverMemory;
import com.zj.aiagemt.service.rag.RagAnswerAdvisor;
import com.zj.aiagemt.utils.SpringContextUtil;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.HashMap;
import java.util.Map;

/**
 * 顾问类型枚举
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/19 09:02
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum AiClientAdvisorTypeEnumVO {

    CHAT_MEMORY("ChatMemory", "上下文记忆（内存模式）") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            AiClientAdvisorVO.ChatMemory chatMemory = aiClientAdvisorVO.getChatMemory();
            return PromptChatMemoryAdvisor.builder(
                    MessageWindowChatMemory.builder()
                            .maxMessages(chatMemory.getMaxMessages())
                            .build()
            ).build();
        }
    },
    
    RAG_ANSWER("RagAnswer", "知识库") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            AiClientAdvisorVO.RagAnswer ragAnswer = aiClientAdvisorVO.getRagAnswer();
            return new RagAnswerAdvisor(vectorStore, SearchRequest.builder()
                    .topK(ragAnswer.getTopK())
                    .filterExpression(ragAnswer.getFilterExpression())
                    .build());
        }
    },

    CONVERSATION_SUMMARY_MEMORY("ConversationSummaryMemoryAdvisor", "对话总结") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            SpringContextUtil springContextUtil = aiClientAdvisorVO.getSpringContextUtil();
            // 创建记忆管理器（负责智能摘要和历史管理）
            ConversationSummaryMemory memory = new ConversationSummaryMemory(springContextUtil);

            return new ConversationSummaryMemoryAdvisor(memory);
        }
    },
    
    VECTOR_STORE_RETRIEVER_MEMORY("VectorStoreRetrieverMemoryAdvisor", "向量存储检索记忆") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            AiClientAdvisorVO.VectorStoreRetriever config = aiClientAdvisorVO.getVectorStoreRetriever();
            
            // 使用配置参数创建向量存储检索记忆管理器
            int topK = config != null ? config.getTopK() : VectorStoreRetrieverMemory.DEFAULT_TOP_K;
            float similarityThreshold = config != null ? config.getSimilarityThreshold() : VectorStoreRetrieverMemory.DEFAULT_SIMILARITY_THRESHOLD;
            
            VectorStoreRetrieverMemory memory = new VectorStoreRetrieverMemory(vectorStore, topK, similarityThreshold);
            return new VectorStoreRetrieverMemoryAdvisor(memory);
        }
    },

    SimpleLoggerAdvisor("SimpleLoggerAdvisor", "日志"){
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {

            return new SimpleLoggerAdvisor();
        }
    },
    ;

    private String code;
    private String info;
    
    // 静态Map缓存，用于快速查找
    private static final Map<String, AiClientAdvisorTypeEnumVO> CODE_MAP = new HashMap<>();
    
    // 静态初始化块，在类加载时初始化Map
    static {
        for (AiClientAdvisorTypeEnumVO enumVO : values()) {
            CODE_MAP.put(enumVO.getCode(), enumVO);
        }
    }
    
    /**
     * 策略方法：创建顾问对象
     * @param aiClientAdvisorVO 顾问配置对象
     * @param vectorStore 向量存储
     * @return 顾问对象
     */
    public abstract Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore);
    
    /**
     * 根据code获取枚举
     * @param code 编码
     * @return 枚举对象
     */
    public static AiClientAdvisorTypeEnumVO getByCode(String code) {
        AiClientAdvisorTypeEnumVO enumVO = CODE_MAP.get(code);
        if (enumVO == null) {
            throw new RuntimeException("err! advisorType " + code + " not exist!");
        }
        return enumVO;
    }

}
