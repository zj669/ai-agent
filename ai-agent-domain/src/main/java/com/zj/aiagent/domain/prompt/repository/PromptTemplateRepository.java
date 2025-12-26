package com.zj.aiagent.domain.prompt.repository;

import java.util.List;

/**
 * 提示词模板存储接口
 * <p>
 * 定义提示词模板的持久化技术接口，具体实现由基础设施层提供
 * （如文件系统、数据库、配置中心等）
 */
public interface PromptTemplateRepository {

    /**
     * 加载提示词模板
     *
     * @param templateKey 模板键
     * @return 模板内容
     */
    String load(String templateKey);

    /**
     * 保存提示词模板
     *
     * @param templateKey     模板键
     * @param templateContent 模板内容
     */
    void save(String templateKey, String templateContent);

    /**
     * 列出所有模板键
     *
     * @param category 分类（可选）
     * @return 模板键列表
     */
    List<String> listKeys(String category);

    /**
     * 删除模板
     *
     * @param templateKey 模板键
     */
    void delete(String templateKey);
}
