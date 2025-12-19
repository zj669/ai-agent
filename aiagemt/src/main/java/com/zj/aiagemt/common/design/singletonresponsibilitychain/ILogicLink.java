package com.zj.aiagemt.common.design.singletonresponsibilitychain;



public interface ILogicLink<REQUEST, CONTEXT, RESULT> extends ILogicChainArmory<REQUEST, CONTEXT, RESULT> {

    RESULT apply(REQUEST request, CONTEXT dynamicContext);

}
