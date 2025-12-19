package com.zj.aiagemt.service.rag.split.similarity;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 欧氏距离相似度计算器
 * 计算两个向量的欧氏距离，并转换为相似度
 * 返回值范围：[0, 1]
 * - 值越大表示越相似
 * - 1 表示完全相同（距离为0）
 * - 接近0表示差异很大
 */
@Component
public class EuclideanDistanceCalculator implements SimilarityCalculator {

    @Override
    public double calculate(List<Double> vector1, List<Double> vector2) {
        if (vector1 == null || vector2 == null) {
            throw new IllegalArgumentException("向量不能为空");
        }

        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException(
                    String.format("向量维度不匹配: %d vs %d", vector1.size(), vector2.size()));
        }

        if (vector1.isEmpty()) {
            return 1.0; // 空向量视为完全相同
        }

        // 计算欧氏距离：sqrt(Σ((v1[i] - v2[i])²))
        double sumSquaredDiff = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            double diff = vector1.get(i) - vector2.get(i);
            sumSquaredDiff += diff * diff;
        }

        double distance = Math.sqrt(sumSquaredDiff);

        // 将距离转换为相似度：1 / (1 + distance)
        // 距离为0时，相似度为1
        // 距离越大，相似度越接近0
        return 1.0 / (1.0 + distance);
    }
}
