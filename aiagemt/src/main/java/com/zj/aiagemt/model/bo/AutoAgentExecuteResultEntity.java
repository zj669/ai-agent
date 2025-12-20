package com.zj.aiagemt.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AutoAgentExecuteResultEntity {

    /**
     * 数据类型：analysis(分析阶段), execution(执行阶段), supervision(监督阶段), summary(总结阶段), error(错误信息), complete(完成标识)
     * 细分类型：analysis_status(任务状态分析), analysis_history(执行历史评估), analysis_strategy(下一步策略), analysis_progress(完成度评估)
     *          execution_target(执行目标), execution_process(执行过程), execution_result(执行结果), execution_quality(质量检查)
     *          supervision_assessment(质量评估), supervision_issues(问题识别), supervision_suggestions(改进建议), supervision_score(质量评分)
     */
    private String type;

    private String nodeName;

    /**
     * 数据内容
     */
    private String content;

    /**
     * 是否完成
     */
    private Boolean completed;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 会话ID
     */
    private String sessionId;
}
