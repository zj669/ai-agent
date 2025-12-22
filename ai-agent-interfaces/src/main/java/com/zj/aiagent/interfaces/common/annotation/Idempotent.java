package com.zj.aiagent.interfaces.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等注解
 * <p>
 * 用于防止用户误触或网络延迟导致的短时间内重复调用接口
 * <p>
 * 使用示例:
 * 
 * <pre>
 * {@code @Idempotent(expireSeconds = 3, message = "请勿重复提交")}
 * public Response<Void> submit(@RequestBody SubmitRequest request) {
 *     // 业务逻辑
 * }
 * </pre>
 *
 * @author zj
 * @since 2025-12-22
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等有效期(秒)
     * <p>
     * 在此时间内对同一用户的相同请求将被拦截
     *
     * @return 过期时间,默认5秒
     */
    long expireSeconds() default 5;

    /**
     * 自定义幂等Key表达式(SpEL)
     * <p>
     * 支持SpEL表达式访问方法参数,如: "#request.orderId"
     * <p>
     * 若为空,则使用默认策略: userId + 方法签名 + 参数Hash
     *
     * @return SpEL表达式
     */
    String key() default "";

    /**
     * 重复请求时的提示消息
     *
     * @return 提示消息
     */
    String message() default "请勿重复提交";
}
