package com.zj.aiagent.domain.agent.valobj;

import lombok.Getter;

@Getter
public enum AgentStatus {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    DISABLED(2, "已下线");

    private final int code;
    private final String desc;

    AgentStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AgentStatus fromCode(int code) {
        for (AgentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown AgentStatus code: " + code);
    }
}
