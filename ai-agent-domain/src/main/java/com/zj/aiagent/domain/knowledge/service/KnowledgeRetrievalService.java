package com.zj.aiagent.domain.knowledge.service;

import java.util.List;

/**
 * 知识检索领域服务
 * 为 SchedulerService 提供长期记忆（LTM）检索能力
 * 自动处理 Agent 权限隔离
 * 
 * 与 SchedulerService 联动：
 * - 在工作流启动前，SchedulerService 调用此服务加载 LTM
 * - 使用 Metadata Filter 确保只检索到属于该 Agent 的知识
 */
public interface KnowledgeRetrievalService {
    /**
     * 根据 Agent ID 检索知识
     * 
     * @param agentId Agent ID（用于权限隔离）
     * @param query   用户查询文本
     * @param topK    返回结果数量
     * @return 相关知识片段列表
     */
    List<String> retrieve(Long agentId, String query, int topK);

    /**
     * 根据 Dataset ID 检索知识（测试用）
     * 
     * @param datasetId 知识库 ID
     * @param query     查询文本
     * @param topK      返回结果数量
     * @return 相关知识片段列表
     */
    List<String> retrieveByDataset(String datasetId, String query, int topK);
}
