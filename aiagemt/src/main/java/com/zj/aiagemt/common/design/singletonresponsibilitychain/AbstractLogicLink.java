package com.zj.aiagemt.common.design.singletonresponsibilitychain;

public abstract class AbstractLogicLink<REQUEST, CONTEXT, RESULT> implements ILogicLink<REQUEST, CONTEXT, RESULT> {

    private ILogicLink<REQUEST, CONTEXT, RESULT> next;

    @Override
    public ILogicLink<REQUEST, CONTEXT, RESULT> next() {
        return next;
    }

    @Override
    public ILogicLink<REQUEST, CONTEXT, RESULT> appendNext(ILogicLink<REQUEST, CONTEXT, RESULT> next) {
        this.next = next;
        return next;
    }

    protected RESULT next(REQUEST request, CONTEXT dynamicContext) {
        return next.apply(request, dynamicContext);
    }

}
