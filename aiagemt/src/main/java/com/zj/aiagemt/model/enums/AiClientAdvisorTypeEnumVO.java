package com.zj.aiagemt.model.enums;


import com.alibaba.fastjson2.JSON;
import com.zj.aiagemt.repository.base.TodoListMapper;
import com.zj.aiagemt.model.vo.AiClientAdvisorVO;
import com.zj.aiagemt.service.memory.ConversationSummaryMemoryAdvisor;
import com.zj.aiagemt.service.memory.VectorStoreRetrieverMemoryAdvisor;
import com.zj.aiagemt.service.memory.chatmemory.ConversationSummaryMemory;
import com.zj.aiagemt.service.memory.chatmemory.VectorStoreRetrieverMemory;
import com.zj.aiagemt.service.rag.RagAnswerAdvisor;
import com.zj.aiagemt.service.tool.TodoListExecuteAdvisor;
import com.zj.aiagemt.service.tool.TodoListPlanAdvisor;
import com.zj.aiagemt.service.todo.TodoListService;
import com.zj.aiagemt.utils.SpringContextUtil;
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
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum AiClientAdvisorTypeEnumVO {

    CHAT_MEMORY("ChatMemory", "上下文记忆（内存模式）") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            String advisorParam = aiClientAdvisorVO.getAdvisorParam();
            AiClientAdvisorVO.ChatMemory chatMemory = JSON.parseObject(advisorParam, AiClientAdvisorVO.ChatMemory.class);
            return PromptChatMemoryAdvisor.builder(
                    MessageWindowChatMemory.builder()
                            .maxMessages(chatMemory.getMaxMessages())
                            .build()
            ).build();
        }
    },

    CONVERSATION_SUMMARY_MEMORY("ConversationSummaryMemory", "上下文记忆（摘要模式）") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            return new ConversationSummaryMemoryAdvisor(new ConversationSummaryMemory(aiClientAdvisorVO.getSpringContextUtil()));
        }
    },

    VECTOR_STORE_RETRIEVER_MEMORY("VectorStoreRetrieverMemory", "向量存储检索记忆") {
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            String advisorParam = aiClientAdvisorVO.getAdvisorParam();
            AiClientAdvisorVO.VectorStoreRetriever config = JSON.parseObject(advisorParam,
                    AiClientAdvisorVO.VectorStoreRetriever.class);
            // 使用配置参数创建向量存储检索记忆管理器
            int topK = config != null ? config.getTopK() : VectorStoreRetrieverMemory.DEFAULT_TOP_K;
            float similarityThreshold = config != null ? config.getSimilarityThreshold() : VectorStoreRetrieverMemory.DEFAULT_SIMILARITY_THRESHOLD;

            VectorStoreRetrieverMemory memory = new VectorStoreRetrieverMemory(vectorStore, topK, similarityThreshold);
            return new VectorStoreRetrieverMemoryAdvisor(memory);
        }
    },
    
    RAG_ANSWER_ADVISOR("RagAnswerAdvisor", "RAG问答"){
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            String advisorParam = aiClientAdvisorVO.getAdvisorParam();
            AiClientAdvisorVO.RagAnswer ragAnswer = JSON.parseObject(advisorParam, AiClientAdvisorVO.RagAnswer.class);
            return new RagAnswerAdvisor(vectorStore, SearchRequest.builder()
                    .topK(ragAnswer.getTopK())
                    .filterExpression(ragAnswer.getFilterExpression())
                    .build());
        }
    },

    SIMPLE_LOGGER_ADVISOR("SimpleLoggerAdvisor", "日志"){
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {

            return new SimpleLoggerAdvisor();
        }
    },

    TODOLIST_PLAN_ADVISOR("TodoListPlanAdvisor", "规划节点"){
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            SpringContextUtil springContextUtil = aiClientAdvisorVO.getSpringContextUtil();
            // 获取TodoListMapper bean
            TodoListMapper todoListMapper = springContextUtil.getBean(TodoListMapper.class);
            TodoListService todoListService = springContextUtil.getBean(TodoListService.class);
            return new TodoListPlanAdvisor(todoListMapper, todoListService);
        }
    },

    TODOLIST_EXECUTE_ADVISOR("TodoListExecuteAdvisor", "规划执行节点"){
        @Override
        public Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO, VectorStore vectorStore) {
            SpringContextUtil springContextUtil = aiClientAdvisorVO.getSpringContextUtil();
            // 获取TodoListMapper bean
            TodoListMapper todoListMapper = springContextUtil.getBean(TodoListMapper.class);
            TodoListService todoListService = springContextUtil.getBean(TodoListService.class);
            return new TodoListExecuteAdvisor(todoListMapper, todoListService);
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
    
    public static AiClientAdvisorTypeEnumVO getByCode(String code) {
        return CODE_MAP.get(code);
    }
}