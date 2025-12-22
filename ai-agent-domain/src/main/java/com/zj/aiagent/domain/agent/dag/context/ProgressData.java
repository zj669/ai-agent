package com.zj.aiagent.domain.agent.dag.context;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 进度数据
 * 封装 DAG 执行进度相关数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressData {

    /** 已完成节点数 */
    private int completedNodes;

    /** 总节点数 */
    private int totalNodes;

    /**
     * 增加已完成节点数
     */
    public void incrementCompleted() {
        completedNodes++;
    }

    /**
     * 计算完成百分比
     */
    public int getPercentage() {
        return totalNodes > 0 ? (completedNodes * 100) / totalNodes : 0;
    }

    /**
     * 检查是否全部完成
     */
    public boolean isComplete() {
        return totalNodes > 0 && completedNodes >= totalNodes;
    }

    /**
     * 获取剩余节点数
     */
    public int getRemainingNodes() {
        return Math.max(0, totalNodes - completedNodes);
    }

    /**
     * 创建新的进度数据
     */
    public static ProgressData of(int completedNodes, int totalNodes) {
        return new ProgressData(completedNodes, totalNodes);
    }
}
