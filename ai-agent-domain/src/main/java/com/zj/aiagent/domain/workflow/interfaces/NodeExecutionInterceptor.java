package com.zj.aiagent.domain.workflow.interfaces;

import com.zj.aiagent.domain.workflow.entity.InterceptResult;
import com.zj.aiagent.domain.workflow.entity.NodeExecutionContext;
import com.zj.aiagent.shared.design.workflow.StateUpdate;

/**
 * 节点执行拦截器接口
 * <p>
 * 支持在节点执行前后进行拦截，实现人工介入、限流、日志等横切关注点
 * </p>
 */
public interface NodeExecutionInterceptor {

    /**
     * 节点执行前拦截
     *
     * @param context 执行上下文
     * @return 拦截结果
     */
    InterceptResult beforeExecution(NodeExecutionContext context);

    /**
     * 节点执行后拦截
     *
     * @param context 执行上下文
     * @param update  节点执行结果
     * @return 拦截结果
     */
    InterceptResult afterExecution(NodeExecutionContext context, StateUpdate update);

    /**
     * 拦截器优先级
     * <p>
     * 数字越小优先级越高
     * </p>
     * <p>
     * 优先级参考：
     * </p>
     * <ul>
     * <li>50: 限流拦截器（最高优先级）</li>
     * <li>100: 人工介入拦截器</li>
     * <li>Integer.MAX_VALUE: 日志拦截器（最低优先级）</li>
     * </ul>
     *
     * @return 优先级
     */
    default int getOrder() {
        return 0;
    }
}
