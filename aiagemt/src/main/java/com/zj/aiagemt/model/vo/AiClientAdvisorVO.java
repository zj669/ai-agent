package com.zj.aiagemt.model.vo;

import com.zj.aiagemt.utils.SpringContextUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiClientAdvisorVO {

    /**
     * 顾问ID
     */
    private String advisorId;

    /**
     * 顾问名称
     */
    private String advisorName;

    /**
     * 顾问类型(PromptChatMemory/RagAnswer/SimpleLoggerAdvisor等)
     */
    private String advisorType;

    private String advisorParam;

    /**
     * 顺序号
     */
    private Integer orderNum;

    /**
     * 扩展；记忆
     */
    private ChatMemory chatMemory;

    /**
     * 扩展；rag 问答
     */
    private RagAnswer ragAnswer;
    
    /**
     * 扩展；向量存储检索记忆
     */
    private VectorStoreRetriever vectorStoreRetriever;

    private SpringContextUtil springContextUtil;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatMemory {
        private int maxMessages;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RagAnswer {
        private int topK = 4;
        private String filterExpression;
    }
    
    /**
     * 向量存储检索记忆配置
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VectorStoreRetriever {
        /** 检索时返回的最大结果数量 */
        private int topK = 5;
        
        /** 相似度匹配阈值，范围[0.0, 1.0] */
        private float similarityThreshold = 0.7f;
        
        /** 过滤表达式，用于限定检索范围 */
        private String filterExpression;
    }

}
