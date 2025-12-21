package com.zj.aiagent.shared.design.ruletree.factory;




import com.zj.aiagent.shared.design.ruletree.AbstractMultiThreadStrategyRouter;
import lombok.Data;


@Data
public abstract class DefaultMultiThreadStrategyFactory<Request, Context, Result> {

    public abstract AbstractMultiThreadStrategyRouter<Request, Context, Result> getRootNode();

}
