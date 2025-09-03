package com.zj.aiagemt.common.design.ruletree.factory;



import com.zj.aiagemt.common.design.ruletree.AbstractStrategyRouter;
import com.zj.aiagemt.common.design.ruletree.StrategyHandler;

import lombok.Data;


@Data
public abstract class DefaultStrategyFactory<Request, Context, Result> {

    public abstract AbstractStrategyRouter<Request, Context, Result> getRootNode();

    public StrategyHandler<Request, Context, Result> strategyHandler() {
        return getRootNode();
    }

}
