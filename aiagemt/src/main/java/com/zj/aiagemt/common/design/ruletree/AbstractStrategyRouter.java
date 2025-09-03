package com.zj.aiagemt.common.design.ruletree;

import lombok.Getter;
import lombok.Setter;

/**
 * 策略路由抽象类
 *
 * @author: wuqingshan
 * @since : 2025年6月26日 15:12:12
 */
public abstract class AbstractStrategyRouter<Request, Context, Result> implements StrategyMapper<Request, Context, Result>, StrategyHandler<Request, Context, Result> {

    @Getter
    @Setter
    protected StrategyHandler<Request, Context, Result> defaultStrategyHandler = StrategyHandler.DEFAULT;

    /**
     * 路由策略
     *
     * @param request        入参
     * @param dynamicContext 上下文
     * @return 返参
     * @throws Exception 异常
     */
    public Result router(Request request, Context dynamicContext) throws Exception {
        StrategyHandler<Request, Context, Result> strategyHandler = get(request, dynamicContext);
        if (null != strategyHandler) return strategyHandler.apply(request, dynamicContext);
        return defaultStrategyHandler.apply(request, dynamicContext);
    }

}
