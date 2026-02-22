package com.zj.aiagent.domain.knowledge.port;

import java.util.List;

/**
 * 文本分块端口
 * 将长文本按配置切分为小块
 */
public interface TextSplitterPort {

    /**
     * 分块
     *
     * @param texts     待分块的文本列表
     * @param chunkSize 每块大小（token 数）
     * @param overlap   重叠 token 数
     * @return 分块后的文本列表
     */
    List<String> split(List<String> texts, int chunkSize, int overlap);
}
