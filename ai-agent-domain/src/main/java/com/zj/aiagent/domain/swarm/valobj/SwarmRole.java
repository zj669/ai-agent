package com.zj.aiagent.domain.swarm.valobj;

import lombok.Getter;

/**
 * Swarm Agent 角色枚举。
 *
 * <p>角色决定 Agent 的行为模式和可用工具：
 * <ul>
 *   <li>COORDINATOR - 协调者（直接服务用户的根 Agent），负责规划和调度</li>
 *   <li>WORKER - 执行者，只负责执行 Coordinator 分配的单一任务</li>
 *   <li>ASSISTANT - 默认助手角色（向后兼容）</li>
 * </ul>
 *
 * <p>语义变更（v2）：
 * <ul>
 *   <li>初始 Agent 的 parentId = null，直接服务用户 = COORDINATOR</li>
 *   <li>子 Agent 的 parentId = 被派发者 = WORKER</li>
 *   <li>ROOT 角色已移除（由 COORDINATOR 替代）</li>
 * </ul>
 *
 * @see SwarmToolFilter 工具白名单过滤
 */
@Getter
public enum SwarmRole {
    COORDINATOR("coordinator", "协调者"),
    WORKER("worker", "执行者"),
    ASSISTANT("assistant", "助手");

    private final String code;
    private final String desc;

    SwarmRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SwarmRole fromCode(String code) {
        for (SwarmRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return ASSISTANT;
    }

    /**
     * 判断此角色是否为调度类（可派发任务/创建子 Agent）。
     */
    public boolean isDispatcher() {
        return this == COORDINATOR;
    }

    /**
     * 判断此角色是否为执行类（只执行任务，不派发）。
     */
    public boolean isWorker() {
        return this == WORKER;
    }
}
