package com.zj.aiagent.domain.swarm.valobj;

import lombok.Getter;

@Getter
public enum SwarmAgentStatus {
    IDLE("IDLE", "空闲"),
    BUSY("BUSY", "忙碌"),
    WAITING("WAITING", "等待中"),
    WAKING("WAKING", "唤醒中"),
    STOPPED("STOPPED", "已停止");

    private final String code;
    private final String desc;

    SwarmAgentStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SwarmAgentStatus fromCode(String code) {
        for (SwarmAgentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return IDLE;
    }
}
