package com.zj.aiagent.domain.agent.dag.context;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 用户输入数据
 * 封装用户输入相关的所有数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInputData {

    /** 原始用户输入 */
    private String userInput;

    /** 用户消息（可能经过处理） */
    private String userMessage;

    /**
     * 获取有效的用户输入
     * 优先返回 userInput，如果为空则返回 userMessage
     */
    public String getEffectiveInput() {
        if (userInput != null && !userInput.isEmpty()) {
            return userInput;
        }
        return userMessage != null ? userMessage : "";
    }

    /**
     * 获取有效的用户输入，带默认值
     */
    public String getEffectiveInput(String defaultValue) {
        String effective = getEffectiveInput();
        return effective.isEmpty() ? defaultValue : effective;
    }
}
