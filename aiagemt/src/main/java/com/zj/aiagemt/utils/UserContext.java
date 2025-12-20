package com.zj.aiagemt.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户上下文工具类
 * 使用ThreadLocal存储当前登录用户ID
 */
@Slf4j
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
        log.debug("设置当前用户ID: {}", userId);
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID,如果未登录则返回null
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 清除当前用户信息
     */
    public static void clear() {
        Long userId = USER_ID_HOLDER.get();
        USER_ID_HOLDER.remove();
        log.debug("清除用户上下文,用户ID: {}", userId);
    }

    /**
     * 检查是否已登录
     *
     * @return true-已登录, false-未登录
     */
    public static boolean isLogin() {
        return USER_ID_HOLDER.get() != null;
    }
}
