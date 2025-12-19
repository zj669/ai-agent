package com.zj.aiagemt.common.design.bizlogic;

import java.util.List;


public abstract class AbstractBizLogicDelegate<CONTEXT> implements BizLogicProcessor<CONTEXT> {

    public void process(CONTEXT context) {
        init(context);
        validate(context);
        fill(context);
        handle(context);
        post(context);
    }

    public abstract List<BizLogicProcessor<CONTEXT>> getBizLogicProcessors();

    @Override
    public void init(CONTEXT context) {
        getBizLogicProcessors().forEach(d -> d.init(context));
    }

    @Override
    public void validate(CONTEXT context) {
        getBizLogicProcessors().forEach(d -> d.validate(context));
    }

    @Override
    public void fill(CONTEXT context) {
        getBizLogicProcessors().forEach(d -> d.fill(context));
    }

    @Override
    public void handle(CONTEXT context) {
        getBizLogicProcessors().forEach(d -> d.handle(context));
    }

    @Override
    public void post(CONTEXT context) {
        getBizLogicProcessors().forEach(d -> d.post(context));
    }

}
