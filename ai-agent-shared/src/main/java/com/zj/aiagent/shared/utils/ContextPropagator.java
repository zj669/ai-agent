package com.zj.aiagent.shared.utils;





import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 上下文传递装饰器 - 共享内核
 * 
 * <p>
 * 解决在异步、响应式编程中ThreadLocal上下文丢失的问题。
 * 在新线程执行前捕获父线程的上下文,执行时恢复上下文,执行后清理上下文。
 * 
 * <h3>使用场景：</h3>
 * <ul>
 * <li>@Async异步方法</li>
 * <li>CompletableFuture异步任务</li>
 * <li>线程池提交的任务</li>
 * <li>DAG节点的并行执行</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * 
 * <pre>{@code
 * // 场景1: CompletableFuture
 * Long currentUserId = UserContext.getUserId();
 * CompletableFuture.supplyAsync(
 *         ContextPropagator.decorateSupplier(() -> {
 *             // 这里可以正常获取userId
 *             Long userId = UserContext.getUserId();
 *             return someService.process(userId);
 *         }, currentUserId));
 * 
 * // 场景2: 线程池
 * executor.submit(ContextPropagator.decorateRunnable(() -> {
 *     Long userId = UserContext.getUserId();
 *     // 业务逻辑...
 * }, UserContext.getUserId()));
 * 
 * // 场景3: @Async方法（需要配置TaskDecorator）
 * // 见 AsyncContextPropagationConfig 配置类
 * }</pre>
 * 
 * @see UserContext
 * @author zj
 * @since 2025-12-21
 */
public class ContextPropagator {

    /**
     * 装饰Runnable,传递用户上下文
     * 
     * @param task   要执行的任务
     * @param userId 当前用户ID（从父线程捕获）
     * @return 装饰后的Runnable
     */
    public static Runnable decorateRunnable(Runnable task, Long userId) {
        return () -> {
            // 执行前设置上下文
            UserContext.setUserId(userId);
            try {
                task.run();
            } finally {
                // 执行后清理上下文
                UserContext.clear();
            }
        };
    }

    /**
     * 装饰Callable,传递用户上下文
     * 
     * @param task   要执行的任务
     * @param userId 当前用户ID（从父线程捕获）
     * @return 装饰后的Callable
     */
    public static <T> Callable<T> decorateCallable(Callable<T> task, Long userId) {
        return () -> {
            UserContext.setUserId(userId);
            try {
                return task.call();
            } finally {
                UserContext.clear();
            }
        };
    }

    /**
     * 装饰Supplier,传递用户上下文
     * 
     * @param supplier 要执行的Supplier
     * @param userId   当前用户ID（从父线程捕获）
     * @return 装饰后的Supplier
     */
    public static <T> Supplier<T> decorateSupplier(Supplier<T> supplier, Long userId) {
        return () -> {
            UserContext.setUserId(userId);
            try {
                return supplier.get();
            } finally {
                UserContext.clear();
            }
        };
    }

    /**
     * 自动捕获当前上下文并装饰Runnable
     * 
     * @param task 要执行的任务
     * @return 装饰后的Runnable
     */
    public static Runnable wrapRunnable(Runnable task) {
        Long userId = UserContext.getUserId();
        return decorateRunnable(task, userId);
    }

    /**
     * 自动捕获当前上下文并装饰Callable
     * 
     * @param task 要执行的任务
     * @return 装饰后的Callable
     */
    public static <T> Callable<T> wrapCallable(Callable<T> task) {
        Long userId = UserContext.getUserId();
        return decorateCallable(task, userId);
    }

    /**
     * 自动捕获当前上下文并装饰Supplier
     * 
     * @param supplier 要执行的Supplier
     * @return 装饰后的Supplier
     */
    public static <T> Supplier<T> wrapSupplier(Supplier<T> supplier) {
        Long userId = UserContext.getUserId();
        return decorateSupplier(supplier, userId);
    }
}
