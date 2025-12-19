package com.zj.aiagemt.service.rag.split.similarity;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 余弦相似度计算器
 * 余弦相似度衡量两个向量之间的夹角余弦值
 * 返回值范围：[-1, 1]
 * - 1 表示完全相同（夹角0度）
 * - 0 表示正交（夹角90度）
 * - -1 表示完全相反（夹角180度）
 */
@Component
public class CosineSimilarityCalculator implements SimilarityCalculator {

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
            return 0.0;
        }

        // 计算点积和向量模
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            double v1 = vector1.get(i);
            double v2 = vector2.get(i);

            dotProduct += v1 * v2;
            magnitude1 += v1 * v1;
            magnitude2 += v2 * v2;
        }

        // 计算向量模的平方根
        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        // 避免除零
        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }

        // 返回余弦相似度
        return dotProduct / (magnitude1 * magnitude2);
    }
}
