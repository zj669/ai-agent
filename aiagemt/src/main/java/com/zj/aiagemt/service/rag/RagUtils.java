package com.zj.aiagemt.service.rag;

import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.utils.SpringContextUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RagUtils {
    @Resource
    private SpringContextUtil springContextUtil;

    public void getAdvisor(VectorStore vectorStore){

        ChatClient chatClient = springContextUtil.getBean(AiAgentEnumVO.AI_CLIENT.getBeanName("3001"));
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                // 重写
                .queryTransformers(RewriteQueryTransformer.builder()
                        .chatClientBuilder(chatClient.mutate())
                        .build())
                .queryTransformers()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(vectorStore)
                        .build())
                .build();

        Query query = Query.builder()
                .text("And what is its second largest city?")
                .history(new UserMessage("What is the capital of Denmark?"),
                        new AssistantMessage("Copenhagen is the capital of Denmark."))
                .build();
        // 压缩查询，将当前问题和实例上下文分开
        QueryTransformer queryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClient.mutate())
                .build();

        Query transformedQuery = queryTransformer.transform(query);

        // 扩展相关问题，将当前问题扩展为多个问题，再去查询
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClient.mutate())
                .includeOriginal(false)
                .numberOfQueries(3)
                .build();
        List<Query> queries = queryExpander.expand(new Query("How to run a Spring Boot app?"));

        // 查询，正常的查询，默认是可以配置相关性算法的，在配置文件种操作
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.73)
                .topK(5)
                .filterExpression(new FilterExpressionBuilder()
                        .eq("genre", "fairytale")
                        .build())
                .build();
        List<Document> documents = retriever.retrieve(new Query("What is the main character of the story?"));
        // 动态床底值
        DocumentRetriever retriever1 = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(() -> new FilterExpressionBuilder()
                        .eq("tenant", transformedQuery.text())
                        .build())
                .build();
        List<Document> documents1 = retriever.retrieve(new Query("What are the KPIs for the next semester?"));

        // 合并文档
        Map<Query, List<List<Document>>> documentsForQuery = new HashMap<>();
        DocumentJoiner documentJoiner = new ConcatenationDocumentJoiner();
        List<Document> documentss = documentJoiner.join(documentsForQuery);
    }
}
