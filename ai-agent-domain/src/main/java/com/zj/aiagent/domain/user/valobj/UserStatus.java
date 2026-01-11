package com.zj.aiagent.domain.user.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {
    DISABLED(0, "禁用"),
    NORMAL(1, "正常");

    private final int code;
    private final String description;

    public static UserStatus fromCode(Integer code) {
        if (code == null)
            return NORMAL;
        for (UserStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return NORMAL;
    }
}
