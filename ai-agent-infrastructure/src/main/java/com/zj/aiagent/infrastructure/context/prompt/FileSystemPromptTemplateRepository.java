package com.zj.aiagent.infrastructure.context.prompt;

import com.zj.aiagent.domain.prompt.repository.PromptTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PromptTemplate Repository 文件系统实现
 * <p>
 * 从文件系统加载提示词模板
 */
@Slf4j
@Repository
public class FileSystemPromptTemplateRepository implements PromptTemplateRepository {

    private static final String TEMPLATES_BASE_PATH = "ai-agent-infrastructure/src/main/resources/prompts/";

    @Override
    public String load(String templateKey) {
        try {
            Path path = Path.of(TEMPLATES_BASE_PATH + templateKey + ".txt");
            if (Files.exists(path)) {
                String content = Files.readString(path);
                log.debug("[FileSystem] 加载模板: {} ({} 字符)", templateKey, content.length());
                return content;
            } else {
                log.warn("[FileSystem] 模板不存在: {}", templateKey);
            }
        } catch (IOException e) {
            log.error("[FileSystem] 加载模板失败: {}", templateKey, e);
        }
        return "";
    }

    @Override
    public void save(String templateKey, String templateContent) {
        // TODO: 保存到文件系统
        log.info("[FileSystem] 保存模板: {} (暂未实现)", templateKey);
    }

    @Override
    public List<String> listKeys(String category) {
        // TODO: 从文件系统扫描
        log.debug("[FileSystem] 列出模板: category={} (暂未实现)", category);
        return new ArrayList<>();
    }

    @Override
    public void delete(String templateKey) {
        // TODO: 从文件系统删除
        log.info("[FileSystem] 删除模板: {} (暂未实现)", templateKey);
    }
}
