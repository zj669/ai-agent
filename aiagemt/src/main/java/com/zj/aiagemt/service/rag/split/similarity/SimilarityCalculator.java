package com.zj.aiagemt.service.rag.split.similarity;

import java.util.List;

/**
 * 相似度计算器接口
 * 使用策略模式支持多种相似度算法
 */
public interface SimilarityCalculator {

    /**
     * 计算两个向量的相似度
     * 
     * @param vector1 第一个向量
     * @param vector2 第二个向量
     * @return 相似度分数，取值范围由具体实现决定
     * @throws IllegalArgumentException 如果两个向量长度不同
     */
    double calculate(List<Double> vector1, List<Double> vector2);
}
