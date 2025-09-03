package com.zj.aiagemt.common.design.ruletree;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


public abstract class AbstractMultiThreadStrategyRouter<Request, Context, Result> implements StrategyMapper<Request, Context, Result>, StrategyHandler<Request, Context, Result> {

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
        if (Objects.nonNull(strategyHandler)) {
            return strategyHandler.apply(request, dynamicContext);
        }
        return getDefaultStrategyHandler().apply(request, dynamicContext);
    }

    @Override
    public Result apply(Request request, Context dynamicContext) throws Exception {
        // 异步加载数据
        multiThread(request, dynamicContext);
        // 业务流程受理
        return doApply(request, dynamicContext);
    }

    /**
     * 异步加载数据
     */
    protected abstract void multiThread(Request request, Context dynamicContext) throws ExecutionException, InterruptedException, TimeoutException;

    /**
     * 业务流程受理
     */
    protected abstract Result doApply(Request request, Context dynamicContext) throws Exception;


}
