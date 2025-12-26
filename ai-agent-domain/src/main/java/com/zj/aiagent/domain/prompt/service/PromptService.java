package com.zj.aiagent.domain.prompt.service;

import com.zj.aiagent.domain.prompt.PromptProvider;
import com.zj.aiagent.domain.prompt.repository.PromptTemplateRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 领域服务
 * <p>
 * 实现提示词管理的业务逻辑，依赖技术接口（Repository）
 */
@Slf4j
@Service
@AllArgsConstructor
public class PromptService implements PromptProvider {

    private final PromptTemplateRepository promptTemplateRepository;

    /**
     * 变量占位符正则：匹配 {varName} 格式
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\w+)\\}");

    @Override
    public String loadPromptTemplate(String templateKey) {
        log.debug("加载提示词模板: {}", templateKey);
        return promptTemplateRepository.load(templateKey);
    }

    @Override
    public String renderPrompt(String templateKey, Map<String, Object> variables) {
        String template = loadPromptTemplate(templateKey);

        if (template == null || template.isEmpty()) {
            log.warn("模板不存在或为空: {}", templateKey);
            return "";
        }

        // 替换占位符
        String rendered = replacePlaceholders(template, variables);

        log.debug("渲染提示词: {} -> {} 字符", templateKey, rendered.length());
        return rendered;
    }

    @Override
    public void savePromptTemplate(String templateKey, String templateContent) {
        log.info("保存提示词模板: {}", templateKey);
        promptTemplateRepository.save(templateKey, templateContent);
    }

    @Override
    public List<String> listTemplates(String category) {
        log.debug("列出模板: category={}", category);
        return promptTemplateRepository.listKeys(category);
    }

    // ========== 私有业务逻辑方法 ==========

    /**
     * 替换模板中的占位符
     *
     * @param template  模板内容
     * @param variables 变量Map
     * @return 替换后的内容
     */
    private String replacePlaceholders(String template, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            String replacement = value != null ? value.toString() : "";

            // 防止特殊字符导致错误
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
