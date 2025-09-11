package com.zj.aiagemt.common.design.strategymode;


/**
 * 通用策略模式
 *
 * @author: wuqingshan
 * @since : 2025年6月26日 15:12:12
 */
public interface StrategyHandler<Request, Result> extends StrategyMapper {

    /**
     * 执行策略
     *
     * @param request 入参
     * @return 返参
     */
    Result apply(Request request);

}