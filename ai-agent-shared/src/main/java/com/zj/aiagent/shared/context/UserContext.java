package com.zj.aiagent.shared.context;

/**
 * 用户上下文
 * <p>
 * 用于在当前线程中存储用户信息（如 UserID）
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前用户 ID
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 清除当前上下文
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
