package com.zj.aiagent.domain.swarm.valobj;

import lombok.Getter;

@Getter
public enum SwarmMessageType {
    TEXT("text", "文本消息");

    private final String code;
    private final String desc;

    SwarmMessageType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
