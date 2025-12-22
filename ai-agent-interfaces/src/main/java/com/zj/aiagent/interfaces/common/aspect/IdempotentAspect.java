package com.zj.aiagent.interfaces.common.aspect;

import com.zj.aiagent.infrastructure.idempotent.IIdempotentService;
import com.zj.aiagent.interfaces.common.annotation.Idempotent;
import com.zj.aiagent.shared.exception.IdempotentException;
import com.zj.aiagent.shared.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 幂等切面
 * <p>
 * 拦截标注了@Idempotent注解的方法，通过Redis分布式锁防止重复调用
 *
 * @author zj
 * @since 2025-12-22
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final IIdempotentService idempotentService;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 生成幂等Key
        String idempotentKey = generateKey(joinPoint, idempotent);

        // 尝试获取幂等锁
        boolean acquired = idempotentService.tryAcquire(idempotentKey, idempotent.expireSeconds());

        if (!acquired) {
            log.warn("幂等拦截 - 重复请求被拦截: key={}", idempotentKey);
            throw new IdempotentException(idempotent.message());
        }

        try {
            // 执行业务逻辑
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 业务异常时释放锁，允许重试
            if (shouldReleaseOnError(e)) {
                idempotentService.release(idempotentKey);
                log.debug("业务异常，释放幂等锁: key={}", idempotentKey);
            }
            throw e;
        }
    }

    /**
     * 生成幂等Key
     * <p>
     * 优先使用SpEL表达式，否则使用默认策略: userId:方法签名:参数Hash
     */
    private String generateKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取用户ID
        Long userId = UserContext.getUserId();
        String userPart = userId != null ? String.valueOf(userId) : "anonymous";

        // 方法签名
        String methodPart = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        // 自定义Key表达式
        String customKey = idempotent.key();
        if (StringUtils.hasText(customKey)) {
            String spelValue = parseSpel(customKey, method, joinPoint.getArgs());
            return userPart + ":" + methodPart + ":" + spelValue;
        }

        // 默认策略: 参数Hash
        String paramsPart = generateParamsHash(joinPoint.getArgs());

        return userPart + ":" + methodPart + ":" + paramsPart;
    }

    /**
     * 解析SpEL表达式
     */
    private String parseSpel(String expression, Method method, Object[] args) {
        try {
            String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
            if (paramNames == null || paramNames.length == 0) {
                return expression;
            }

            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                ((StandardEvaluationContext) context).setVariable(paramNames[i], args[i]);
            }

            Object value = PARSER.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.warn("SpEL表达式解析失败: {}, 使用原始表达式", expression, e);
            return expression;
        }
    }

    /**
     * 生成参数Hash
     */
    private String generateParamsHash(Object[] args) {
        if (args == null || args.length == 0) {
            return "empty";
        }

        String paramsStr = Arrays.toString(args);
        return DigestUtils.md5DigestAsHex(paramsStr.getBytes(StandardCharsets.UTF_8)).substring(0, 8);
    }

    /**
     * 判断是否需要在异常时释放锁
     * <p>
     * 业务异常时释放锁允许用户重试，系统异常保持锁防止雪崩
     */
    private boolean shouldReleaseOnError(Throwable e) {
        // 可以根据具体业务定义哪些异常需要释放锁
        // 默认: 业务异常(IllegalArgumentException等)释放锁
        return e instanceof IllegalArgumentException
                || e instanceof IllegalStateException;
    }
}
