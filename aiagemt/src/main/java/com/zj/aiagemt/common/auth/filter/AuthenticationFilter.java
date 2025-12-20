package com.zj.aiagemt.common.auth.filter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证过滤器接口
 * 
 * <p>
 * 定义认证过滤器的统一规范，支持多种认证方式的组合使用。
 * 过滤器将按照优先级顺序执行，任一过滤器认证成功即停止后续过滤器的执行。
 * 
 * @author zj
 * @since 2025-12-20
 */
public interface AuthenticationFilter {

    /**
     * 尝试认证
     * 
     * <p>
     * 过滤器应检查请求中是否包含其需要的认证信息，如果包含则进行认证。
     * 
     * @param request HTTP请求对象
     * @return 认证成功返回用户ID，未处理或认证失败返回null
     * @throws com.zj.aiagemt.common.auth.exception.AuthenticationException 认证信息格式错误或验证失败时抛出
     */
    Long authenticate(HttpServletRequest request);

    /**
     * 过滤器是否启用
     * 
     * <p>
     * 只有启用的过滤器才会被加入到认证过滤链中。
     * 可以通过配置文件动态控制过滤器的启用状态。
     * 
     * @return true-启用，false-禁用
     */
    boolean isEnabled();

    /**
     * 过滤器执行优先级
     * 
     * <p>
     * 数值越小优先级越高，优先级高的过滤器会先执行。
     * 建议的优先级范围：
     * <ul>
     * <li>1-50: 高优先级（如Debug、测试用途）</li>
     * <li>51-100: 中优先级（如OAuth、API Key）</li>
     * <li>101-200: 低优先级（如JWT、Session）</li>
     * </ul>
     * 
     * @return 优先级值
     */
    int getOrder();

    /**
     * 过滤器名称
     * 
     * <p>
     * 用于日志记录和调试，应返回一个简短且有意义的名称。
     * 
     * @return 过滤器名称
     */
    String getName();
}
