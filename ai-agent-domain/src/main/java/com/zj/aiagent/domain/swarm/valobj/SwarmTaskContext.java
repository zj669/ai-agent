package com.zj.aiagent.domain.swarm.valobj;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Swarm Task Context — 值对象。
 *
 * <p>记录 Worker Agent 在执行任务过程中探索过的文件和发现。
 * 用于支撑 Coordinator 的 Continue vs Spawn 决策。
 *
 * <p>此对象由 Worker 在每次 ReAct 循环后更新，
 * 通过 EventBus 广播给 Coordinator，供上下文重叠度分析使用。
 *
 * <p>设计原则：
 * <ul>
 *   <li>Domain 层纯净对象，只依赖 shared 模块</li>
 *   <li>不可变值对象，所有修改返回新实例</li>
 *   <li>线程安全（操作级别 volatile）</li>
 * </ul>
 */
@Data
@Builder
public class SwarmTaskContext {

    /** 关联的 Agent ID */
    private final Long agentId;

    /** 探索过的文件路径集合 */
    @Builder.Default
    private final Set<String> exploredFiles = new HashSet<>();

    /** 探索过的模块集合 */
    @Builder.Default
    private final Set<String> exploredModules = new HashSet<>();

    /** 关键发现列表 */
    @Builder.Default
    private final java.util.List<String> findings = new java.util.ArrayList<>();

    /** 当前 Phase */
    @Builder.Default
    private final Phase currentPhase = Phase.RESEARCH;

    /** 最后更新时间 */
    private final Instant lastUpdated;

    /**
     * Phase 枚举，与 SwarmAgentRunner.Phase 保持一致。
     * 在 domain 层定义是为了避免循环依赖。
     */
    public enum Phase {
        RESEARCH("Research", "调研阶段：收集信息、分析问题"),
        SYNTHESIS("Synthesis", "整合阶段：汇总发现、撰写规格"),
        IMPLEMENTATION("Implementation", "实施阶段：执行变更、实现功能"),
        VERIFICATION("Verification", "验证阶段：测试验证、确认正确性");

        private final String label;
        private final String description;

        Phase(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() { return label; }
        public String getDescription() { return description; }

        /**
         * Phase 流转顺序：RESEARCH → SYNTHESIS → IMPLEMENTATION → VERIFICATION
         */
        public Phase next() {
            return switch (this) {
                case RESEARCH -> SYNTHESIS;
                case SYNTHESIS -> IMPLEMENTATION;
                case IMPLEMENTATION -> VERIFICATION;
                case VERIFICATION -> VERIFICATION; // 终态
            };
        }

        /**
         * 判断是否可以流转到目标 Phase（只能向前流转，不能倒退）
         */
        public boolean canTransitionTo(Phase target) {
            return target.ordinal() > this.ordinal();
        }
    }

    // ── 工厂方法 ────────────────────────────────────────────────

    /**
     * 创建新的任务上下文（初始状态）
     */
    public static SwarmTaskContext create(Long agentId) {
        return SwarmTaskContext.builder()
            .agentId(agentId)
            .exploredFiles(new HashSet<>())
            .exploredModules(new HashSet<>())
            .findings(new java.util.ArrayList<>())
            .currentPhase(Phase.RESEARCH)
            .lastUpdated(Instant.now())
            .build();
    }

    // ── 不可变修改方法（返回新实例）────────────────────────────────

    /**
     * 添加探索过的文件路径
     * @param file 文件路径（相对或绝对路径）
     * @return 新的上下文实例
     */
    public SwarmTaskContext addExploredFile(String file) {
        if (file == null || file.isBlank()) {
            return this;
        }
        Set<String> newFiles = new HashSet<>(this.exploredFiles);
        newFiles.add(file.trim());
        return SwarmTaskContext.builder()
            .agentId(this.agentId)
            .exploredFiles(newFiles)
            .exploredModules(new HashSet<>(this.exploredModules))
            .findings(new java.util.ArrayList<>(this.findings))
            .currentPhase(this.currentPhase)
            .lastUpdated(Instant.now())
            .build();
    }

    /**
     * 添加多个探索过的文件路径
     */
    public SwarmTaskContext addExploredFiles(Iterable<String> files) {
        if (files == null) {
            return this;
        }
        Set<String> newFiles = new HashSet<>(this.exploredFiles);
        for (String file : files) {
            if (file != null && !file.isBlank()) {
                newFiles.add(file.trim());
            }
        }
        return SwarmTaskContext.builder()
            .agentId(this.agentId)
            .exploredFiles(newFiles)
            .exploredModules(new HashSet<>(this.exploredModules))
            .findings(new java.util.ArrayList<>(this.findings))
            .currentPhase(this.currentPhase)
            .lastUpdated(Instant.now())
            .build();
    }

    /**
     * 添加探索过的模块
     */
    public SwarmTaskContext addExploredModule(String module) {
        if (module == null || module.isBlank()) {
            return this;
        }
        Set<String> newModules = new HashSet<>(this.exploredModules);
        newModules.add(module.trim());
        return SwarmTaskContext.builder()
            .agentId(this.agentId)
            .exploredFiles(new HashSet<>(this.exploredFiles))
            .exploredModules(newModules)
            .findings(new java.util.ArrayList<>(this.findings))
            .currentPhase(this.currentPhase)
            .lastUpdated(Instant.now())
            .build();
    }

    /**
     * 添加关键发现
     */
    public SwarmTaskContext addFinding(String finding) {
        if (finding == null || finding.isBlank()) {
            return this;
        }
        java.util.List<String> newFindings = new java.util.ArrayList<>(this.findings);
        newFindings.add(finding.trim());
        return SwarmTaskContext.builder()
            .agentId(this.agentId)
            .exploredFiles(new HashSet<>(this.exploredFiles))
            .exploredModules(new HashSet<>(this.exploredModules))
            .findings(newFindings)
            .currentPhase(this.currentPhase)
            .lastUpdated(Instant.now())
            .build();
    }

    /**
     * 转换到新的 Phase
     */
    public SwarmTaskContext withPhase(Phase newPhase) {
        return SwarmTaskContext.builder()
            .agentId(this.agentId)
            .exploredFiles(new HashSet<>(this.exploredFiles))
            .exploredModules(new HashSet<>(this.exploredModules))
            .findings(new java.util.ArrayList<>(this.findings))
            .currentPhase(newPhase)
            .lastUpdated(Instant.now())
            .build();
    }

    // ── 查询方法 ────────────────────────────────────────────────

    /**
     * 合并另一个上下文的探索结果
     */
    public SwarmTaskContext merge(SwarmTaskContext other) {
        if (other == null) {
            return this;
        }
        Set<String> mergedFiles = new HashSet<>(this.exploredFiles);
        mergedFiles.addAll(other.exploredFiles);

        Set<String> mergedModules = new HashSet<>(this.exploredModules);
        mergedModules.addAll(other.exploredModules);

        java.util.List<String> mergedFindings = new java.util.ArrayList<>(this.findings);
        mergedFindings.addAll(other.findings);

        return SwarmTaskContext.builder()
            .agentId(this.agentId)
            .exploredFiles(mergedFiles)
            .exploredModules(mergedModules)
            .findings(mergedFindings)
            .currentPhase(this.currentPhase)
            .lastUpdated(Instant.now())
            .build();
    }

    /**
     * 导出为结构化描述字符串
     */
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("探索文件数: ").append(exploredFiles.size());
        if (!exploredFiles.isEmpty()) {
            sb.append(" (");
            sb.append(String.join(", ",
                exploredFiles.stream().limit(5).toList()));
            if (exploredFiles.size() > 5) {
                sb.append("...+").append(exploredFiles.size() - 5).append(" more");
            }
            sb.append(")");
        }
        sb.append("\n探索模块数: ").append(exploredModules.size());
        if (!exploredModules.isEmpty()) {
            sb.append(" (");
            sb.append(String.join(", ", exploredModules));
            sb.append(")");
        }
        sb.append("\n发现数: ").append(findings.size());
        return sb.toString();
    }
}
