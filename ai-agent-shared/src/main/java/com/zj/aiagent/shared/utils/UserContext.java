package com.zj.aiagent.shared.utils;


import java.util.logging.Logger;

/**
 * 用户上下文工具类 - 共享内核
 * 
 * 使用ThreadLocal存储当前登录用户ID，用于在方法调用链中传递用户身份信息
 * 
 * <h3>使用场景：</h3>
 * <ul>
 * <li>在Filter/Interceptor中设置当前登录用户ID</li>
 * <li>在Service层获取当前用户信息，无需层层传递参数</li>
 * <li>在Audit日志中记录操作人员</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * 
 * <pre>{@code
 * // 在认证过滤器中设置
 * public void doFilter(ServletRequest request, ServletResponse response) {
 *     Long userId = extractUserIdFromToken(request);
 *     UserContext.setUserId(userId);
 *     try {
 *         chain.doFilter(request, response);
 *     } finally {
 *         UserContext.clear(); // 务必清理，避免内存泄漏
 *     }
 * }
 * 
 * // 在Service中获取
 * public void updateResource(Resource resource) {
 *     Long currentUserId = UserContext.getUserId();
 *     resource.setUpdatedBy(currentUserId);
 *     // ...
 * }
 * }</pre>
 * 
 * <h3>注意事项：</h3>
 * <ul>
 * <li><b>必须清理：</b>请求结束后必须调用clear()，避免ThreadLocal内存泄漏</li>
 * <li><b>异步场景：</b>在使用@Async、CompletableFuture等异步方式时，ThreadLocal会丢失，需要显式传递userId</li>
 * <li><b>响应式编程：</b>在WebFlux等响应式框架中无效，应使用Reactor Context</li>
 * <li><b>线程池场景：</b>跨线程时需要使用InheritableThreadLocal或手动传递</li>
 * </ul>
 * 
 * @see java.lang.ThreadLocal
 */

public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    public static Logger logger = Logger.getLogger(UserContext.class.getName());

    /**
     * 设置当前用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
        logger.info("设置当前用户ID: " + userId);
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
     * 获取当前用户ID，如果未登录则抛出异常
     * 
     * @return 用户ID
     * @throws IllegalStateException 如果未登录
     */
    public static Long requireUserId() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null) {
            throw new IllegalStateException("用户未登录，无法获取用户ID");
        }
        return userId;
    }

    /**
     * 清除当前用户信息
     * 
     * <b>重要：</b>必须在请求结束时调用，通常在Filter的finally块中
     */
    public static void clear() {
        logger.info("清除当前用户信息, 当前用户id："+USER_ID_HOLDER.get());
        USER_ID_HOLDER.remove();
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
