package com.zj.aiagent.domain.swarm.valobj;

import lombok.Getter;

/**
 * Swarm Agent 角色枚举。
 *
 * <p>角色决定 Agent 的行为模式和可用工具：
 * <ul>
 *   <li>ROOT - 根 Agent（用户直接交互的主 Agent），拥有所有工具</li>
 *   <li>COORDINATOR - 协调者，负责规划和调度，不执行具体任务</li>
 *   <li>WORKER - 执行者，只负责执行 Coordinator 分配的单一任务</li>
 *   <li>HUMAN - 人类代理，无工具</li>
 *   <li>ASSISTANT - 默认助手角色（向后兼容）</li>
 * </ul>
 *
 * @see SwarmToolFilter 工具白名单过滤
 */
@Getter
public enum SwarmRole {
    ROOT("root", "根Agent"),
    COORDINATOR("coordinator", "协调者"),
    WORKER("worker", "执行者"),
    HUMAN("human", "人类"),
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
        return this == ROOT || this == COORDINATOR;
    }

    /**
     * 判断此角色是否为执行类（只执行任务，不派发）。
     */
    public boolean isWorker() {
        return this == WORKER;
    }

    /**
     * 判断此角色是否拥有全部工具。
     */
    public boolean isFullAccess() {
        return this == ROOT;
    }
}
