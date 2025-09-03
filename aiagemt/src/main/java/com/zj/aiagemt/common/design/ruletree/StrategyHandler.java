package com.zj.aiagemt.common.design.ruletree;

/**
 * 规则树handler
 *
 * @author: wuqingshan
 * @since : 2025年6月26日 15:12:12
 */
public interface StrategyHandler<Request, Context, Result> {

    StrategyHandler DEFAULT = (T, D) -> null;

    /**
     * 执行策略
     *
     * @param request        入参
     * @param dynamicContext 上下文
     * @return 返参
     * @throws Exception 异常
     */
    Result apply(Request request, Context dynamicContext) throws Exception;

}