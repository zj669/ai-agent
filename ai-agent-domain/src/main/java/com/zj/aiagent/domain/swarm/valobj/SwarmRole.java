package com.zj.aiagent.domain.swarm.valobj;

import lombok.Getter;

@Getter
public enum SwarmRole {
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
        // 自定义角色也允许，返回 null 或 ASSISTANT 兜底
        return ASSISTANT;
    }
}
