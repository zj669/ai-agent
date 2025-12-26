package com.zj.aiagent.domain.prompt;

import java.util.List;
import java.util.Map;

public interface PromptProvider {
    String loadPromptTemplate(String templateKey);

    String renderPrompt(String templateKey, Map<String, Object> variables);

    void savePromptTemplate(String templateKey, String templateContent);

    List<String> listTemplates(String category);
}
