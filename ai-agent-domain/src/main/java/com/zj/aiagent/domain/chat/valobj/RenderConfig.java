package com.zj.aiagent.domain.chat.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 前端渲染配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenderConfig {
    /**
     * 显示模式：
     * - HIDDEN: 隐藏
     * - THOUGHT: 思考过程
     * - MESSAGE: 正式回复
     */
    private String mode;

    /**
     * 标题（如"正在思考..."）
     */
    private String title;
}
