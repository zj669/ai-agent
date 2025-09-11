package com.zj.aiagemt.service.rag.rank;

import org.springframework.ai.document.Document;

import java.util.List;

public interface IReRankCaculate {

    List<Document> rank(List<Document>... documentLists);
    
    // 或者可以提供另一种方法签名，接受List<List<Document>>参数gte-rerank-v2
    // List<Document> rank(List<List<Document>> documentLists);
}