package com.zj.aiagent.interfaces.common.interceptor;

/**
 * 认证策略接口
 */
public interface AuthStrategy {
    boolean authenticate(String token, Object... extraArgs);
}
