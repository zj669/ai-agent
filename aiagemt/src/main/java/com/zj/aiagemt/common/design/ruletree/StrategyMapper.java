package com.zj.aiagemt.common.design.ruletree;


public interface StrategyMapper<Request, Context, Result> {

    /**
     * 获取待执行策略
     *
     * @param request        入参
     * @param dynamicContext 上下文
     * @return 返参
     * @throws Exception 异常
     */
    StrategyHandler<Request, Context, Result> get(Request request, Context dynamicContext) throws Exception;
}
