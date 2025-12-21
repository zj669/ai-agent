package com.zj.aiagent.shared.design.strategymode;



public interface StrategyHandler<Request, Result> extends StrategyMapper {

    /**
     * 执行策略
     *
     * @param request 入参
     * @return 返参
     */
    Result apply(Request request);

}